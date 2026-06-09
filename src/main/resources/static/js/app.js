/**
 * SPARTA SHOP — SPA (Single Page Application)
 * 해시 기반 라우팅으로 모든 페이지를 #app 에 렌더링합니다.
 */

// ================================================================
// STATE
// ================================================================
const S = {
  portoneConfig:    null,
  pendingCartIds:   null,
  pendingPreview:   null,
  pendingDirectBuy: null,
  paymentResult:    null,
  currentItems:     [],
  pageCb:           null,
  currentCategory:  '',
};

// ================================================================
// UTILS
// ================================================================
function fmtPrice(n) {
  if (n == null) return '0';
  return Number(n).toLocaleString('ko-KR');
}

function fmtDate(s) {
  if (!s) return '-';
  const d = new Date(s);
  const pad = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}.${pad(d.getMonth()+1)}.${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

const ORDER_STATUS = {
  PAYMENT_PENDING: '결제 대기',
  COMPLETED:       '결제 완료',
  CANCELED:        '취소됨',
};
const PAY_STATUS = {
  READY:             '결제 준비',
  PAID:              '결제 완료',
  CANCELLED:         '취소됨',
  PARTIAL_REFUNDED:  '부분 환불',
  REFUNDED:          '전액 환불',
  FAILED:            '실패',
};
const POINT_TYPE = { EARN: '적립', USE: '사용', USE_CANCEL: '사용 취소 (환불)', EARN_CANCEL: '적립 취소 (환불)' };

const statusLabel   = s => ORDER_STATUS[s]  || s || '-';
const payLabel      = s => PAY_STATUS[s]    || s || '-';
const ptLabel       = s => POINT_TYPE[s]    || s || '-';

// ================================================================
// DOM HELPERS
// ================================================================
const $app   = () => document.getElementById('app');
const $id    = (id) => document.getElementById(id);

function setHTML(html) { $app().innerHTML = html; }

function loading() {
  setHTML(`<div class="loading-wrap"><div class="spinner"></div><p>로딩 중...</p></div>`);
}

// ================================================================
// NAVIGATION
// ================================================================
function go(path) { location.hash = '#' + path; }

// ================================================================
// TOAST
// ================================================================
function toast(msg, type = 'info') {
  const c = $id('toast-container');
  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  el.textContent = msg;
  c.appendChild(el);
  requestAnimationFrame(() => el.classList.add('show'));
  setTimeout(() => {
    el.classList.remove('show');
    setTimeout(() => el.remove(), 300);
  }, 3200);
}

// ================================================================
// MODAL
// ================================================================
function modal(html, onOk) {
  const overlay = $id('modal-overlay');
  const box     = $id('modal-box');
  box.innerHTML = html;
  overlay.style.display = 'flex';

  box.querySelector('[data-confirm]')?.addEventListener('click', onOk);
  box.querySelector('[data-cancel]')?.addEventListener('click', closeModal);
  overlay.onclick = e => { if (e.target === overlay) closeModal(); };
}

function closeModal() {
  $id('modal-overlay').style.display = 'none';
  $id('modal-box').innerHTML = '';
}

// ================================================================
// HEADER
// ================================================================
function updateHeader() {
  const navAuth = $id('nav-auth');
  if (!navAuth) return;

  if (API.isLoggedIn()) {
    const m = API.member.get();
    navAuth.innerHTML = `
      <span class="nav-user">${m?.name || m?.email || '회원'}님</span>
      <button class="btn-text" onclick="App.doLogout()">로그아웃</button>
    `;
  } else {
    navAuth.innerHTML = `
      <a href="#/login"  class="btn-sm-outline">로그인</a>
      <a href="#/signup" class="btn-sm-primary">회원가입</a>
    `;
  }

  // 장바구니 뱃지
  if (API.isLoggedIn()) {
    API.getCart()
      .then(c => {
        const cnt = c?.items?.reduce((s, i) => s + i.quantity, 0) || 0;
        const badge = $id('cart-badge');
        if (badge) {
          badge.textContent = cnt;
          badge.style.display = cnt > 0 ? 'inline-flex' : 'none';
        }
      })
      .catch(() => {});
  } else {
    const badge = $id('cart-badge');
    if (badge) badge.style.display = 'none';
  }
}

function requireAuth() {
  if (!API.isLoggedIn()) {
    toast('로그인이 필요합니다.', 'warning');
    go('/login');
    return false;
  }
  return true;
}

// ================================================================
// ROUTER
// ================================================================
function route() {
  const hash  = location.hash.replace('#', '') || '/';
  const parts = hash.split('/').filter(Boolean);
  const page  = parts[0] || '';
  const id    = parts[1];

  // active nav 표시
  document.querySelectorAll('.nav-link').forEach(a => {
    const pg = a.dataset.page;
    a.classList.toggle('active',
      pg === '/' ? page === '' : hash.startsWith(pg)
    );
  });

  switch (page) {
    case '':         S.currentCategory = ''; return renderProducts();
    case 'products': return id ? renderProductDetail(id) : renderProducts();
    case 'cart':           return renderCart();
    case 'order-preview':  return renderOrderPreview();
    case 'direct-preview': return renderDirectPreview();
    case 'orders':   return id ? renderOrderDetail(id) : renderOrders();
    case 'points':   return renderPoints();
    case 'login':    return renderLogin();
    case 'signup':   return renderSignup();
    case 'payment-success': return renderPaymentSuccess();
    default:         return renderProducts();
  }
}

// ================================================================
// PAGE: 상품 목록
// ================================================================
const CATEGORY_LABELS = { CLOTHES: '의류', FOOD: '식품', ELECTRONICS: '전자제품' };

async function renderProducts(page = 0, category = S.currentCategory) {
  S.currentCategory = category;
  loading();
  try {
    const data = await API.getProducts(page);
    let products = data?.content || [];

    if (category) {
      products = products.filter(p => p.category === category);
    }

    const pageTitle = category ? (CATEGORY_LABELS[category] || category) : '전체 상품';

    const catBtns = ['', 'CLOTHES', 'FOOD', 'ELECTRONICS'].map(c => {
      const label = c ? (CATEGORY_LABELS[c] || c) : '전체';
      const active = c === category ? ' cat-btn-active' : '';
      return `<button class="cat-btn${active}" onclick="renderProducts(0,'${c}')">${label}</button>`;
    }).join('');

    const cards = products.length === 0
      ? `<div class="empty-state"><p>등록된 상품이 없습니다.</p></div>`
      : products.map(p => {
        const soldOut = p.stockQuantity <= 0;
        const catName = CATEGORY_LABELS[p.category] || p.category || '상품';
        const thumb = p.imageUrl
          ? `<img class="product-img" src="${p.imageUrl}" alt="${p.name}" loading="lazy">`
          : `<div class="product-thumb cat-${(p.category||'default').toLowerCase()}"><span>${catName}</span></div>`;
        return `
          <div class="product-card">
            <div class="product-thumb-wrap">${thumb}</div>
            <div class="product-body">
              <p class="product-cat">${catName}</p>
              <h3 class="product-name" title="${p.name}">${p.name}</h3>
              <p class="product-price">${fmtPrice(p.price)}<span class="price-won">원</span></p>
              <p class="product-stock ${soldOut ? 'soldout' : ''}">${soldOut ? '품절' : `재고 ${p.stockQuantity}개`}</p>
            </div>
            <div class="product-actions">
              <button class="btn-sm-outline content"
                onclick="go('/products/${p.productId}')">상세보기</button>
              ${!soldOut ? `<button class="btn-sm-primary"
                onclick="App.quickCart(${p.productId}, '${p.name.replace(/'/g,'\\\'')}')"
              >장바구니</button>` : ''}
            </div>
          </div>`;
      }).join('');

    setHTML(`
      <div class="page-header">
        <h1 class="page-title">${pageTitle}</h1>
        <span class="item-count">총 ${products.length}개</span>
      </div>
      <div class="cat-filter-row">${catBtns}</div>
      <div class="product-grid">${cards}</div>
    `);
  } catch (e) {
    toast(e.message, 'error');
    setHTML(`<div class="error-state"><p>상품을 불러오지 못했습니다.</p></div>`);
  }
}

// ================================================================
// PAGE: 상품 상세
// ================================================================
async function renderProductDetail(id) {
  loading();
  try {
    const p = await API.getProduct(id);
    const soldOut = p.stockQuantity <= 0;

    setHTML(`
      <div class="back-row">
        <button class="btn-back" onclick="history.back()">← 목록으로</button>
      </div>
      <div class="detail-wrap">
        <div class="detail-thumb-wrap">
          ${p.imageUrl
            ? `<img class="detail-img" src="${p.imageUrl}" alt="${p.name}">`
            : `<div class="detail-thumb cat-${(p.category||'default').toLowerCase()}"><span>${CATEGORY_LABELS[p.category] || p.category || '상품'}</span></div>`
          }
        </div>
        <div class="detail-info">
          <p class="product-cat">${p.category || ''}</p>
          <h1 class="detail-name">${p.name}</h1>
          <p class="detail-price">${fmtPrice(p.price)}<span class="price-won">원</span></p>
          ${p.description ? `<p class="detail-desc">${p.description}</p>` : ''}
          <p class="product-stock ${soldOut ? 'soldout' : ''}">${soldOut ? '품절' : `재고 ${p.stockQuantity}개`}</p>

          ${!soldOut ? `
            <div class="qty-row">
              <span class="qty-label">수량</span>
              <div class="qty-ctrl">
                <button class="qty-btn" onclick="adjQty(-1)">−</button>
                <input type="number" id="pQty" value="1" min="1" max="${p.stockQuantity}" class="qty-input">
                <button class="qty-btn" onclick="adjQty(1)">+</button>
              </div>
            </div>
            <div class="detail-btns">
              <button class="btn-outline" onclick="App.addCartDetail(${p.productId})">장바구니 담기</button>
              <button class="btn-primary" onclick="App.directBuy(${p.productId})">바로 구매</button>
            </div>
          ` : `<button class="btn-disabled" disabled>품절</button>`}
        </div>
      </div>
    `);
  } catch (e) {
    toast(e.message, 'error');
    setHTML(`<div class="error-state"><p>상품 정보를 불러오지 못했습니다.</p></div>`);
  }
}

function adjQty(d) {
  const el = $id('pQty');
  if (!el) return;
  el.value = Math.max(1, Math.min(+el.max, +el.value + d));
}

// ================================================================
// PAGE: 장바구니
// ================================================================
async function renderCart() {
  if (!requireAuth()) return;
  loading();
  try {
    const cart  = await API.getCart();
    const items = cart?.items || [];

    if (items.length === 0) {
      setHTML(`
        <div class="page-header"><h1 class="page-title">장바구니</h1></div>
        <div class="empty-state">
          <p>장바구니가 비어있습니다.</p>
          <a href="#/" class="btn-primary" style="margin-top:16px">쇼핑하러 가기</a>
        </div>`);
      return;
    }

    const rows = items.map(it => `
      <div class="cart-row" data-id="${it.id}">
        <input type="checkbox" class="cart-chk" value="${it.id}" checked
          onchange="calcCartTotal()">
        <div class="cart-info">
          <p class="cart-name">${it.productName}</p>
          <p class="cart-unit">${fmtPrice(it.price)}원 × 1개</p>
        </div>
        <div class="cart-qty">
          <button class="qty-btn" onclick="App.setCartQty(${it.id},${it.quantity-1})">−</button>
          <span class="qty-val">${it.quantity}</span>
          <button class="qty-btn" onclick="App.setCartQty(${it.id},${it.quantity+1})">+</button>
        </div>
        <p class="cart-sub">${fmtPrice(it.price * it.quantity)}원</p>
        <button class="cart-del" onclick="App.delCartItem(${it.id})" title="삭제">✕</button>
      </div>`).join('');

    setHTML(`
      <div class="page-header">
        <h1 class="page-title">장바구니</h1>
        <span class="item-count">${items.length}개 상품</span>
      </div>
      <div class="cart-layout">
        <div class="cart-list">
          <label class="select-all-row">
            <input type="checkbox" id="selAll" checked onchange="toggleAll(this.checked)">
            전체 선택
          </label>
          ${rows}
        </div>
        <aside class="cart-aside">
          <h3>주문 금액</h3>
          <div class="summary-row">
            <span>상품 금액</span><span id="cartSub">${fmtPrice(cart.totalAmount)}원</span>
          </div>
          <div class="summary-div"></div>
          <div class="summary-row bold">
            <span>합계</span><span id="cartTotal">${fmtPrice(cart.totalAmount)}원</span>
          </div>
          <button class="btn-primary btn-block" onclick="App.toPreview()">주문하기</button>
        </aside>
      </div>
    `);
  } catch (e) {
    toast(e.message, 'error');
  }
}

function calcCartTotal() {
  let total = 0;
  document.querySelectorAll('.cart-chk:checked').forEach(cb => {
    const row = cb.closest('.cart-row');
    const sub = row.querySelector('.cart-sub').textContent.replace(/[^0-9]/g, '');
    total += parseInt(sub) || 0;
  });
  const sub   = $id('cartSub');
  const grand = $id('cartTotal');
  if (sub)   sub.textContent   = fmtPrice(total) + '원';
  if (grand) grand.textContent = fmtPrice(total) + '원';
}

function toggleAll(checked) {
  document.querySelectorAll('.cart-chk').forEach(cb => { cb.checked = checked; });
  calcCartTotal();
}

// ================================================================
// PAGE: 주문 미리보기
// ================================================================
async function renderOrderPreview() {
  if (!requireAuth()) return;
  if (!S.pendingCartIds) { go('/cart'); return; }

  loading();
  try {
    const [preview, pointData] = await Promise.all([
      API.getOrderPreview(S.pendingCartIds, 0),
      API.getPointBalance().catch(() => ({ pointBalance: 0 })),
    ]);

    S.pendingPreview  = preview;
    const balance = pointData?.pointBalance || 0;

    const itemRows = (preview?.items || []).map(it => `
      <div class="preview-item">
        <span class="pi-name">${it.productName}</span>
        <span class="pi-qty">× ${it.quantity}</span>
        <span class="pi-price">${fmtPrice(it.subtotal)}원</span>
      </div>`).join('');

    setHTML(`
      <div class="back-row">
        <button class="btn-back" onclick="history.back()">← 장바구니</button>
      </div>
      <h1 class="page-title" style="margin-bottom:20px">주문 확인</h1>
      <div class="preview-layout">
        <div class="preview-main">
          <div class="preview-section">
            <h3 class="sec-title">주문 상품</h3>
            ${itemRows}
          </div>
          <div class="preview-section">
            <h3 class="sec-title">배송지</h3>
            <p class="preview-ship">기본 배송지 (배송 기능은 추후 제공 예정)</p>
          </div>
        </div>
        <aside class="preview-aside">
          <h3 class="sec-title">결제 금액</h3>
          <div class="summary-row">
            <span>상품 금액</span><span>${fmtPrice(preview.totalAmount)}원</span>
          </div>
          <div class="pt-use-row">
            <div class="pt-balance">보유 포인트 <strong>${fmtPrice(balance)}P</strong></div>
            <div class="pt-input-row">
              <input type="number" id="ptInput" value="0" min="0" max="${balance}"
                placeholder="사용할 포인트" class="pt-input"
                oninput="App.updateDiscount(${preview.totalAmount})">
              <button class="btn-sm-outline" onclick="App.useAllPt(${balance})">전액</button>
            </div>
          </div>
          <div class="summary-row discount">
            <span>포인트 할인</span><span id="ptDiscount">-0원</span>
          </div>
          <div class="summary-div"></div>
          <div class="summary-row bold">
            <span>결제 금액</span><span id="pgTotal">${fmtPrice(preview.pgAmount)}원</span>
          </div>
          <p class="earn-hint" id="earnHint">결제 완료 시 약 <strong>${fmtPrice(Math.floor(preview.pgAmount / 100))}P</strong> 적립</p>
          <button class="btn-primary btn-block btn-pay" id="payBtn" onclick="App.startPay()">
            결제하기
          </button>
        </aside>
      </div>
    `);
  } catch (e) {
    toast(e.message, 'error');
    go('/cart');
  }
}

// ================================================================
// PAGE: 주문 목록
// ================================================================
async function renderOrders(page = 0) {
  if (!requireAuth()) return;
  loading();
  try {
    const data   = await API.getOrders(page);
    const orders = data?.content || [];

    if (orders.length === 0) {
      setHTML(`
        <div class="page-header"><h1 class="page-title">주문 내역</h1></div>
        <div class="empty-state">
          <p>주문 내역이 없습니다.</p>
          <a href="#/" class="btn-primary" style="margin-top:16px">쇼핑하러 가기</a>
        </div>`);
      return;
    }

    const cards = orders.map(o => `
      <div class="order-card" onclick="go('/orders/${o.orderId}')">
        <div class="oc-head">
          <span class="oc-num">${o.orderNumber}</span>
          <span class="status-badge s-${o.status.toLowerCase()}">${statusLabel(o.status)}</span>
        </div>
        <div class="oc-body">
          <span class="oc-date">${fmtDate(o.createAt)}</span>
          <span class="oc-cnt">${o.itemCount}개 상품</span>
        </div>
        <div class="oc-foot">
          <span class="oc-total">${fmtPrice(o.totalAmount)}원</span>
          <span class="oc-arrow">›</span>
        </div>
      </div>`).join('');

    setHTML(`
      <div class="page-header">
        <h1 class="page-title">주문 내역</h1>
        <span class="item-count">총 ${fmtPrice(data.totalElements)}건</span>
      </div>
      <div class="order-list">${cards}</div>
      ${data.totalPages > 1 ? buildPager(page, data.totalPages, renderOrders) : ''}
    `);
  } catch (e) {
    toast(e.message, 'error');
  }
}

// ================================================================
// PAGE: 주문 상세
// ================================================================
async function renderOrderDetail(id) {
  if (!requireAuth()) return;
  loading();
  try {
    const o = await API.getOrder(id);
    S.currentItems = o.items || [];
    // API 응답의 paymentId를 우선 사용하고, localStorage를 폴백으로 쓴다.
    const paymentId = o.paymentId || API.paymentMap.getPaymentId(id);
    if (o.paymentId) API.paymentMap.save(id, o.paymentId);

    const itemRows = S.currentItems.map(it => `
      <tr>
        <td>${it.productName}</td>
        <td class="tc">${it.quantity}개</td>
        <td class="tr">${fmtPrice(it.price)}원</td>
        <td class="tr">${fmtPrice(it.subtotal)}원</td>
      </tr>`).join('');

    // 상태에 따른 액션 버튼
    let actions = '';
    if (o.status === 'PAYMENT_PENDING') {
      actions = `<button class="btn-danger" onclick="App.doCancelOrder(${o.orderId})">주문 취소</button>`;
    } else if (o.status === 'COMPLETED') {
      if ((o.paymentStatus === 'PAID' || o.paymentStatus === 'PARTIAL_REFUNDED') && paymentId) {
        actions = `
          ${o.paymentStatus === 'PAID'
            ? `<button class="btn-outline" onclick="App.doCancelPayment(${paymentId},${o.orderId})">결제 취소</button>`
            : ''}
          <button class="btn-danger" onclick="App.openRefund(${paymentId})">환불 신청</button>`;
      } else if (!paymentId && (o.paymentStatus === 'PAID' || o.paymentStatus === 'PARTIAL_REFUNDED')) {
        actions = `<p class="no-payid-hint">이 주문의 결제 ID는 현재 세션에 없습니다. 환불은 고객센터로 문의하세요.</p>`;
      }
    }

    setHTML(`
      <div class="back-row">
        <button class="btn-back" onclick="go('/orders')">← 주문 내역</button>
      </div>
      <div class="od-card">
        <div class="od-header">
          <div>
            <h2 class="od-num">${o.orderNumber}</h2>
            <p class="od-date">${fmtDate(o.createAt)}</p>
          </div>
          <span class="status-badge s-${o.status.toLowerCase()}">${statusLabel(o.status)}</span>
        </div>

        <div class="od-section">
          <h3 class="sec-title">주문 상품</h3>
          <div class="table-wrap">
            <table class="data-table">
              <thead><tr><th>상품명</th><th>수량</th><th>단가</th><th>금액</th></tr></thead>
              <tbody>${itemRows}</tbody>
            </table>
          </div>
        </div>

        <div class="od-section">
          <h3 class="sec-title">결제 정보</h3>
          <div class="info-grid">
            <div class="ig-row">
              <span>결제 상태</span>
              <span class="status-badge s-${(o.paymentStatus||'').toLowerCase()}">${payLabel(o.paymentStatus)}</span>
            </div>
            <div class="ig-row"><span>상품 금액</span><span>${fmtPrice(o.totalAmount)}원</span></div>
            <div class="ig-row"><span>포인트 할인</span><span>-${fmtPrice(o.pointAmount)}원</span></div>
            <div class="ig-row bold"><span>실결제 금액</span><span>${fmtPrice(o.pgAmount)}원</span></div>
            ${o.earnedPointAmount ? `<div class="ig-row"><span>적립 포인트</span><span class="pt-pos">+${fmtPrice(o.earnedPointAmount)}P</span></div>` : ''}
          </div>
        </div>

        ${(o.refunds && o.refunds.length > 0) ? `
        <div class="od-section">
          <h3 class="sec-title">환불 내역</h3>
          <div class="table-wrap">
            <table class="data-table">
              <thead><tr><th>일시</th><th>사유</th><th>환불 금액</th><th>PG 환불</th><th>포인트 환불</th></tr></thead>
              <tbody>
                ${o.refunds.map(r => `
                  <tr>
                    <td>${fmtDate(r.createdAt)}</td>
                    <td>${r.reason}</td>
                    <td class="tr">${fmtPrice(r.totalRefundAmount)}원</td>
                    <td class="tr">${fmtPrice(r.pgRefundAmount)}원</td>
                    <td class="tr">${fmtPrice(r.pointRefundAmount)}원</td>
                  </tr>`).join('')}
              </tbody>
            </table>
          </div>
        </div>` : ''}

        ${actions ? `<div class="od-actions">${actions}</div>` : ''}
      </div>
    `);
  } catch (e) {
    toast(e.message, 'error');
  }
}

// ================================================================
// PAGE: 포인트
// ================================================================
async function renderPoints(page = 0) {
  if (!requireAuth()) return;
  loading();
  try {
    const [bal, hist] = await Promise.all([
      API.getPointBalance(),
      API.getPointHistories(page),
    ]);

    const balance  = bal?.pointBalance || 0;
    const rows     = hist?.content || [];

    const histRows = rows.length === 0
      ? `<tr><td colspan="4" class="tc empty-row">포인트 내역이 없습니다.</td></tr>`
      : rows.map(h => {
          const isNeg = h.amount < 0;
          return `
        <tr>
          <td>${fmtDate(h.createdAt)}</td>
          <td><span class="pt-badge pt-${h.type.toLowerCase()}">${ptLabel(h.type)}</span></td>
          <td class="tr ${isNeg ? 'pt-neg' : 'pt-pos'}">
            ${isNeg ? '-' : '+'}${fmtPrice(Math.abs(h.amount))}P
          </td>
          <td class="tr">${fmtPrice(h.balanceAfter)}P</td>
        </tr>`;
        }).join('');

    setHTML(`
      <div class="page-header"><h1 class="page-title">포인트</h1></div>
      <div class="pt-balance-card">
        <p class="pt-label">보유 포인트</p>
        <p class="pt-amount">${fmtPrice(balance)}<span class="pt-unit">P</span></p>
        <p class="pt-desc">결제 금액의 1%가 자동 적립됩니다.</p>
      </div>
      <div class="section-hd"><h3>포인트 내역</h3></div>
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>일시</th><th>구분</th><th>금액</th><th>잔액</th></tr></thead>
          <tbody>${histRows}</tbody>
        </table>
      </div>
      ${hist?.totalElements > hist?.size ? buildPager(page, Math.ceil(hist.totalElements / hist.size), renderPoints) : ''}
    `);
  } catch (e) {
    toast(e.message, 'error');
  }
}

// ================================================================
// PAGE: 로그인
// ================================================================
function renderLogin() {
  if (API.isLoggedIn()) { go('/'); return; }
  setHTML(`
    <div class="auth-wrap">
      <div class="auth-card">
        <div class="auth-top">
          <h2>로그인</h2>
          <p>SPARTA SHOP에 오신 것을 환영합니다.</p>
        </div>
        <form onsubmit="App.doLogin(event)">
          <div class="field">
            <label>이메일</label>
            <input type="email" name="email" placeholder="email@example.com" autocomplete="username" required>
          </div>
          <div class="field">
            <label>비밀번호</label>
            <input type="password" name="password" placeholder="비밀번호" autocomplete="current-password" required>
          </div>
          <p id="loginErr" class="form-err"></p>
          <button type="submit" class="btn-primary btn-block btn-lg">로그인</button>
        </form>
        <div class="auth-foot">계정이 없으신가요? <a href="#/signup">회원가입</a></div>
      </div>
    </div>
  `);
}

// ================================================================
// PAGE: 회원가입
// ================================================================
function renderSignup() {
  if (API.isLoggedIn()) { go('/'); return; }
  setHTML(`
    <div class="auth-wrap">
      <div class="auth-card">
        <div class="auth-top">
          <h2>회원가입</h2>
          <p>새 계정을 만들어보세요.</p>
        </div>
        <form onsubmit="App.doSignup(event)">
          <div class="field">
            <label>이름</label>
            <input type="text" name="name" placeholder="홍길동" required>
          </div>
          <div class="field">
            <label>이메일</label>
            <input type="email" name="email" placeholder="email@example.com" required>
          </div>
          <div class="field">
            <label>비밀번호</label>
            <input type="password" name="password" placeholder="영문+숫자 6자 이상" required>
            <span class="field-hint">영문과 숫자를 포함하여 6자 이상 입력하세요.</span>
          </div>
          <div class="field">
            <label>전화번호</label>
            <input type="tel" name="phoneNumber" placeholder="010-1234-5678" required>
          </div>
          <p id="signupErr" class="form-err"></p>
          <button type="submit" class="btn-primary btn-block btn-lg">회원가입</button>
        </form>
        <div class="auth-foot">이미 계정이 있으신가요? <a href="#/login">로그인</a></div>
      </div>
    </div>
  `);
}

// ================================================================
// PAGE: 결제 완료
// ================================================================
function renderPaymentSuccess() {
  const d = S.paymentResult;
  if (!d) { go('/orders'); return; }

  setHTML(`
    <div class="success-wrap">
      <div class="success-icon">✓</div>
      <h2 class="success-title">결제 완료!</h2>
      <p class="success-sub">결제가 성공적으로 처리되었습니다.</p>
      <div class="success-card">
        <div class="ig-row"><span>주문 번호</span><span>${d.orderNumber || d.orderId}</span></div>
        <div class="ig-row"><span>결제 금액</span><span>${fmtPrice(d.pgAmount)}원</span></div>
        ${d.pointAmount > 0 ? `<div class="ig-row"><span>포인트 할인</span><span>-${fmtPrice(d.pointAmount)}P</span></div>` : ''}
        <div class="ig-row"><span>적립 포인트</span><span class="pt-pos">+${fmtPrice(d.earnedPointAmount)}P</span></div>
      </div>
      <div class="success-actions">
        <a href="#/orders" class="btn-outline">주문 내역 보기</a>
        <a href="#/"       class="btn-primary">쇼핑 계속하기</a>
      </div>
    </div>
  `);

  S.paymentResult   = null;
  S.pendingCartIds  = null;
  S.pendingPreview  = null;
  updateHeader(); // 장바구니 뱃지 갱신
}

// ================================================================
// ACTIONS — 인증
// ================================================================
async function doLogin(e) {
  e.preventDefault();
  const f   = e.target;
  const btn = f.querySelector('button[type=submit]');
  const err = $id('loginErr');

  btn.disabled = true;
  btn.textContent = '로그인 중...';
  err.textContent = '';

  try {
    await API.login(f.email.value, f.password.value);
    toast('로그인되었습니다.', 'success');
    updateHeader();
    go('/');
  } catch (ex) {
    err.textContent = ex.message;
    btn.disabled = false;
    btn.textContent = '로그인';
  }
}

async function doSignup(e) {
  e.preventDefault();
  const f   = e.target;
  const btn = f.querySelector('button[type=submit]');
  const err = $id('signupErr');

  btn.disabled = true;
  btn.textContent = '처리 중...';
  err.textContent = '';

  try {
    await API.signup(f.email.value, f.password.value, f.name.value, f.phoneNumber.value);
    // 회원가입 성공 후 자동 로그인
    await API.login(f.email.value, f.password.value);
    toast('회원가입이 완료되었습니다.', 'success');
    updateHeader();
    go('/');
  } catch (ex) {
    err.textContent = ex.message;
    btn.disabled = false;
    btn.textContent = '회원가입';
  }
}

async function doLogout() {
  try {
    await API.logout();
    toast('로그아웃되었습니다.', 'info');
    updateHeader();
    go('/');
  } catch (ex) {
    toast(ex.message, 'error');
  }
}

// ================================================================
// ACTIONS — 장바구니
// ================================================================
async function quickCart(productId, productName) {
  if (!requireAuth()) return;
  try {
    await API.addCartItem(productId, 1);
    toast(`"${productName}"을(를) 장바구니에 담았습니다.`, 'success');
    updateHeader();
  } catch (ex) { toast(ex.message, 'error'); }
}

async function addCartDetail(productId) {
  if (!requireAuth()) return;
  const qty = parseInt($id('pQty')?.value || '1', 10);
  try {
    await API.addCartItem(productId, qty);
    toast('장바구니에 담았습니다.', 'success');
    updateHeader();
  } catch (ex) { toast(ex.message, 'error'); }
}

async function setCartQty(cartItemId, newQty) {
  if (newQty < 1) { delCartItem(cartItemId); return; }
  try {
    await API.updateCartItem(cartItemId, newQty);
    renderCart();
  } catch (ex) { toast(ex.message, 'error'); }
}

async function delCartItem(cartItemId) {
  try {
    await API.deleteCartItem(cartItemId);
    toast('삭제했습니다.', 'info');
    renderCart();
    updateHeader();
  } catch (ex) { toast(ex.message, 'error'); }
}

function toPreview() {
  const checked = document.querySelectorAll('.cart-chk:checked');
  const ids = Array.from(checked).map(c => parseInt(c.value, 10));
  if (ids.length === 0) { toast('주문할 상품을 선택해주세요.', 'warning'); return; }
  S.pendingCartIds = ids;
  go('/order-preview');
}

// ================================================================
// ACTIONS — 주문 미리보기
// ================================================================
function updateDiscount(total) {
  const pt = parseInt($id('ptInput')?.value || '0', 10) || 0;
  const pg = Math.max(0, total - pt);
  const discEl = $id('ptDiscount');
  const pgEl   = $id('pgTotal');
  const earnEl = $id('earnHint');
  if (discEl) discEl.textContent = `-${fmtPrice(pt)}원`;
  if (pgEl)   pgEl.textContent   = `${fmtPrice(pg)}원`;
  if (earnEl) earnEl.innerHTML   = `결제 완료 시 약 <strong>${fmtPrice(Math.floor(pg / 100))}P</strong> 적립`;
}

function useAllPt(balance) {
  const el = $id('ptInput');
  if (!el) return;
  el.value = balance;
  el.dispatchEvent(new Event('input'));
}

// ================================================================
// ACTIONS — 바로 구매
// ================================================================
function directBuy(productId) {
  if (!requireAuth()) return;
  const qty = parseInt($id('pQty')?.value || '1', 10);
  S.pendingDirectBuy = { productId, quantity: qty };
  go('/direct-preview');
}

// ================================================================
// PAGE: 바로 구매 주문 확인
// ================================================================
async function renderDirectPreview() {
  if (!requireAuth()) return;
  if (!S.pendingDirectBuy) { go('/'); return; }

  loading();
  try {
    const { productId, quantity } = S.pendingDirectBuy;
    const [product, pointData] = await Promise.all([
      API.getProduct(productId),
      API.getPointBalance().catch(() => ({ pointBalance: 0 })),
    ]);

    const totalAmount = product.price * quantity;
    const balance     = pointData?.pointBalance || 0;

    setHTML(`
      <div class="back-row">
        <button class="btn-back" onclick="history.back()">← 상품으로</button>
      </div>
      <h1 class="page-title" style="margin-bottom:20px">주문 확인</h1>
      <div class="preview-layout">
        <div class="preview-main">
          <div class="preview-section">
            <h3 class="sec-title">주문 상품</h3>
            <div class="preview-item">
              <span class="pi-name">${product.name}</span>
              <span class="pi-qty">× ${quantity}</span>
              <span class="pi-price">${fmtPrice(totalAmount)}원</span>
            </div>
          </div>
          <div class="preview-section">
            <h3 class="sec-title">배송지</h3>
            <p class="preview-ship">기본 배송지 (배송 기능은 추후 제공 예정)</p>
          </div>
        </div>
        <aside class="preview-aside">
          <h3 class="sec-title">결제 금액</h3>
          <div class="summary-row">
            <span>상품 금액</span><span>${fmtPrice(totalAmount)}원</span>
          </div>
          <div class="pt-use-row">
            <div class="pt-balance">보유 포인트 <strong>${fmtPrice(balance)}P</strong></div>
            <div class="pt-input-row">
              <input type="number" id="ptInput" value="0" min="0" max="${balance}"
                placeholder="사용할 포인트" class="pt-input"
                oninput="App.updateDiscount(${totalAmount})">
              <button class="btn-sm-outline" onclick="App.useAllPt(${balance})">전액</button>
            </div>
          </div>
          <div class="summary-row discount">
            <span>포인트 할인</span><span id="ptDiscount">-0원</span>
          </div>
          <div class="summary-div"></div>
          <div class="summary-row bold">
            <span>결제 금액</span><span id="pgTotal">${fmtPrice(totalAmount)}원</span>
          </div>
          <p class="earn-hint" id="earnHint">결제 완료 시 약 <strong>${fmtPrice(Math.floor(totalAmount / 100))}P</strong> 적립</p>
          <button class="btn-primary btn-block btn-pay" id="payBtn" onclick="App.startDirectPay()">
            결제하기
          </button>
        </aside>
      </div>
    `);
  } catch (e) {
    toast(e.message, 'error');
    go('/');
  }
}

async function startDirectPay() {
  const btn = $id('payBtn');
  if (btn) { btn.disabled = true; btn.textContent = '주문 생성 중...'; }

  const { productId, quantity } = S.pendingDirectBuy;
  const pt = parseInt($id('ptInput')?.value || '0', 10) || 0;
  try {
    const order = await API.createProductOrder(productId, quantity, pt);
    await runPortone(order);
  } catch (ex) {
    toast(ex.message, 'error');
    if (btn) { btn.disabled = false; btn.textContent = '결제하기'; }
  }
}

// ================================================================
// ACTIONS — 결제 진행 (장바구니 주문)
// ================================================================
async function startPay() {
  const btn = $id('payBtn');
  if (btn) { btn.disabled = true; btn.textContent = '주문 생성 중...'; }

  const pt = parseInt($id('ptInput')?.value || '0', 10) || 0;
  try {
    const order = await API.createOrder(S.pendingCartIds, pt);
    await runPortone(order);
  } catch (ex) {
    toast(ex.message, 'error');
    if (btn) { btn.disabled = false; btn.textContent = '결제하기'; }
  }
}

// ================================================================
// PortOne 결제 흐름
// ================================================================
async function runPortone(order) {
  // PortOne 설정 로드
  if (!S.portoneConfig) {
    try { S.portoneConfig = await API.getPortOneConfig(); }
    catch { toast('결제 설정을 불러오지 못했습니다.', 'error'); return; }
  }

  // pgAmount가 0이면 PortOne 없이 바로 확정 (포인트 100% 사용)
  if (order.pgAmount === 0) {
    const res = await API.confirmPayment(order.orderId, order.portonePaymentId);
    API.paymentMap.save(order.orderId, res.paymentId);
    S.paymentResult = { ...res, orderNumber: order.orderNumber };
    toast('결제가 완료되었습니다!', 'success');
    go('/payment-success');
    return;
  }

  if (typeof PortOne === 'undefined') {
    toast('결제 SDK를 불러오지 못했습니다. 새로고침 후 다시 시도해주세요.', 'error');
    return;
  }

  const m = API.member.get();
  let portoneRes;
  try {
    portoneRes = await PortOne.requestPayment({
      storeId:     S.portoneConfig.storeId,
      channelKey:  S.portoneConfig.channelKey,
      paymentId:   order.portonePaymentId,
      orderName:   'SPARTA SHOP 주문',
      totalAmount: order.pgAmount,
      currency:    'CURRENCY_KRW',
      payMethod:   'CARD',
      customer: {
        email:       m?.email       || undefined,
        fullName:    m?.name        || undefined,
        phoneNumber: m?.phoneNumber ? m.phoneNumber.replace(/\D/g, '') : undefined,
      },
    });
  } catch (ex) {
    toast(`결제 오류: ${ex.message}`, 'error');
    try { await API.cancelOrder(order.orderId); } catch { /* ignore */ }
    go('/cart');
    return;
  }

  // PortOne 실패/취소
  if (portoneRes?.code) {
    toast(`결제가 취소되었습니다.`, 'warning');
    try { await API.cancelOrder(order.orderId); } catch { /* ignore */ }
    go('/cart');
    return;
  }

  // 결제 성공 → 서버 확정
  try {
    toast('결제 확인 중...', 'info');
    const res = await API.confirmPayment(order.orderId, order.portonePaymentId);
    API.paymentMap.save(order.orderId, res.paymentId);
    S.paymentResult = { ...res, orderNumber: order.orderNumber };
    toast('결제가 완료되었습니다!', 'success');
    go('/payment-success');
  } catch (ex) {
    toast(`결제 확정 실패: ${ex.message}`, 'error');
  }
}

// ================================================================
// ACTIONS — 주문 취소
// ================================================================
function doCancelOrder(orderId) {
  modal(`
    <div class="modal-inner">
      <h3>주문 취소</h3>
      <p style="margin-top:8px;color:#555">정말 이 주문을 취소하시겠습니까?</p>
      <div class="modal-actions">
        <button class="btn-outline" data-cancel>닫기</button>
        <button class="btn-danger"  data-confirm>취소 확인</button>
      </div>
    </div>
  `, async () => {
    closeModal();
    try {
      await API.cancelOrder(orderId);
      toast('주문이 취소되었습니다.', 'success');
      renderOrderDetail(orderId);
    } catch (ex) { toast(ex.message, 'error'); }
  });
}

// ================================================================
// ACTIONS — 결제 취소
// ================================================================
function doCancelPayment(paymentId, orderId) {
  modal(`
    <div class="modal-inner">
      <h3>결제 취소</h3>
      <div class="field" style="margin-top:12px">
        <label>취소 사유</label>
        <input type="text" id="cancelReason" placeholder="사유를 입력하세요" class="form-input">
      </div>
      <div class="modal-actions">
        <button class="btn-outline" data-cancel>닫기</button>
        <button class="btn-danger"  data-confirm>취소 확인</button>
      </div>
    </div>
  `, async () => {
    const reason = $id('cancelReason')?.value || '고객 요청';
    closeModal();
    try {
      await API.cancelPayment(paymentId, reason);
      toast('결제가 취소되었습니다.', 'success');
      renderOrderDetail(orderId);
    } catch (ex) { toast(ex.message, 'error'); }
  });
}

// ================================================================
// ACTIONS — 환불 신청
// ================================================================
function openRefund(paymentId) {
  const items = S.currentItems;
  if (!items.length) { toast('환불할 상품이 없습니다.', 'warning'); return; }

  const rows = items.map(it => `
    <div class="refund-row">
      <label>
        <input type="checkbox" class="refund-chk" data-id="${it.orderItemId}" data-max="${it.quantity}"
          onchange="toggleRefundQty(this)">
        ${it.productName} (최대 ${it.quantity}개)
      </label>
      <input type="number" class="refund-qty" data-id="${it.orderItemId}"
        value="${it.quantity}" min="1" max="${it.quantity}"
        placeholder="수량" style="display:none">
    </div>`).join('');

  modal(`
    <div class="modal-inner">
      <h3>환불 신청</h3>
      <div class="field" style="margin-top:12px">
        <label>환불 사유 <span style="color:var(--primary)">*</span></label>
        <input type="text" id="refundReason" placeholder="환불 사유를 입력하세요" class="form-input">
      </div>
      <div class="field" style="margin-top:12px">
        <label>환불 상품 선택</label>
        ${rows}
      </div>
      <div class="modal-actions">
        <button class="btn-outline" data-cancel>닫기</button>
        <button class="btn-danger"  data-confirm>환불 신청</button>
      </div>
    </div>
  `, async () => {
    const reason = $id('refundReason')?.value?.trim();
    if (!reason) { toast('환불 사유를 입력해주세요.', 'warning'); return; }

    const selected = [];
    document.querySelectorAll('.refund-chk:checked').forEach(cb => {
      const id  = parseInt(cb.dataset.id,  10);
      const qty = parseInt(
        document.querySelector(`.refund-qty[data-id="${id}"]`)?.value || cb.dataset.max, 10
      );
      selected.push({ orderItemId: id, quantity: qty });
    });

    if (!selected.length) { toast('환불할 상품을 선택해주세요.', 'warning'); return; }

    closeModal();
    try {
      await API.requestRefund(paymentId, reason, selected);
      toast('환불 신청이 완료되었습니다.', 'success');
      go('/orders');
    } catch (ex) { toast(ex.message, 'error'); }
  });
}

function toggleRefundQty(cb) {
  const qtyEl = document.querySelector(`.refund-qty[data-id="${cb.dataset.id}"]`);
  if (qtyEl) qtyEl.style.display = cb.checked ? 'block' : 'none';
}

// ================================================================
// PAGINATION
// ================================================================
function buildPager(current, total, cb) {
  S.pageCb = cb;
  const btns = Array.from({ length: total }, (_, i) =>
    `<button class="pg-btn ${i === current ? 'active' : ''}"
      onclick="S.pageCb && S.pageCb(${i})">${i + 1}</button>`
  ).join('');
  return `<div class="pagination">${btns}</div>`;
}

// ================================================================
// APP 공개 인터페이스 (inline onclick 핸들러용)
// ================================================================
const App = {
  doLogin,
  doSignup,
  doLogout,
  quickCart,
  addCartDetail,
  setCartQty,
  delCartItem,
  toPreview,
  updateDiscount,
  useAllPt,
  directBuy,
  startDirectPay,
  startPay,
  doCancelOrder,
  doCancelPayment,
  openRefund,
};

// ================================================================
// INIT
// ================================================================
window.addEventListener('hashchange', route);

document.addEventListener('DOMContentLoaded', () => {
  updateHeader();
  // PortOne 설정 사전 로드
  API.getPortOneConfig()
    .then(cfg => { S.portoneConfig = cfg; })
    .catch(() => {});
  route();
});
