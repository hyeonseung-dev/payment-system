/**
 * SPARTA SHOP — API Client
 * 모든 백엔드 통신은 이 모듈을 통해 이루어집니다.
 */
const API = (() => {
  const TOKEN_KEY   = 'sparta_token';
  const MEMBER_KEY  = 'sparta_member';
  const PMAP_KEY    = 'sparta_pmap';   // orderId → paymentId 매핑

  // ── 로컬스토리지 래퍼 ──────────────────────────────────────

  const token = {
    get:    ()  => localStorage.getItem(TOKEN_KEY),
    set:    (v) => localStorage.setItem(TOKEN_KEY, v),
    remove: ()  => localStorage.removeItem(TOKEN_KEY),
  };

  const member = {
    get: () => {
      try { return JSON.parse(localStorage.getItem(MEMBER_KEY)); }
      catch { return null; }
    },
    set:    (v) => localStorage.setItem(MEMBER_KEY, JSON.stringify(v)),
    remove: ()  => localStorage.removeItem(MEMBER_KEY),
  };

  const paymentMap = {
    _read: () => {
      try { return JSON.parse(localStorage.getItem(PMAP_KEY)) || {}; }
      catch { return {}; }
    },
    getPaymentId: (orderId) => paymentMap._read()[String(orderId)],
    save: (orderId, paymentId) => {
      const m = paymentMap._read();
      m[String(orderId)] = paymentId;
      localStorage.setItem(PMAP_KEY, JSON.stringify(m));
    },
  };

  // ── 공통 fetch 래퍼 ───────────────────────────────────────

  async function req(method, path, body) {
    const headers = { 'Content-Type': 'application/json' };
    const t = token.get();
    if (t) headers['Authorization'] = 'Bearer ' + t;

    const options = { method, headers };
    if (body !== undefined) options.body = JSON.stringify(body);

    const res = await fetch(path, options);

    let json;
    try { json = await res.json(); }
    catch { throw new Error(`서버 응답 오류 (HTTP ${res.status})`); }

    if (!res.ok) throw new Error(json?.message || `요청 실패 (${res.status})`);
    return json?.data;
  }

  // ── 공개 API ─────────────────────────────────────────────

  return {
    token,
    member,
    paymentMap,
    isLoggedIn: () => !!token.get(),

    // 인증
    signup: (email, password, name, phoneNumber) =>
      req('POST', '/api/auth/signup', { email, password, name, phoneNumber }),

    async login(email, password) {
      const data = await req('POST', '/api/auth/login', { email, password });
      if (data?.accessToken) token.set(data.accessToken);
      if (data?.name) member.set({ name: data.name, email, phoneNumber: data.phoneNumber });
      return data;
    },

    async logout() {
      try { await req('POST', '/api/auth/logout'); }
      finally { token.remove(); member.remove(); }
    },

    // 상품
    getProducts: (page = 0, size = 12) =>
      req('GET', `/api/products?page=${page}&size=${size}`),
    getProduct: (id) =>
      req('GET', `/api/products/${id}`),

    // 장바구니
    getCart:         ()              => req('GET',    '/api/carts'),
    addCartItem:     (pid, qty)      => req('POST',   '/api/carts/items',       { productId: pid, quantity: qty }),
    updateCartItem:  (id, qty)       => req('PATCH',  `/api/carts/items/${id}`, { quantity: qty }),
    deleteCartItem:  (id)            => req('DELETE', `/api/carts/items/${id}`),

    // 주문
    getOrderPreview: (cartItemIds, usePointAmount) =>
      req('POST', '/api/orders/preview', {
        cartItemIds,
        usePointAmount: usePointAmount || null,
      }),
    createOrder: (cartItemIds, usePointAmount) =>
      req('POST', '/api/orders', {
        cartItemIds,
        usePointAmount: usePointAmount || null,
      }),
    createProductOrder: (productId, quantity, pointAmount) =>
      req('POST', '/api/orders/products', {
        productId,
        quantity,
        pointAmount: pointAmount || null,
      }),
    getOrders:   (page = 0) => req('GET',  `/api/orders?page=${page}&size=10`),
    getOrder:    (id)        => req('GET',  `/api/orders/${id}`),
    cancelOrder: (id)        => req('POST', `/api/orders/${id}/cancel`),

    // 결제
    confirmPayment: (orderId, portonePaymentId) =>
      req('POST', '/api/payments/confirm', { orderId, portonePaymentId }),
    cancelPayment: (paymentId, reason) =>
      req('POST', `/api/payments/${paymentId}/cancel`, { reason }),

    // 환불
    requestRefund: (paymentId, reason, items) =>
      req('POST', `/api/payments/${paymentId}/refunds`, { reason, items }),

    // 포인트
    getPointBalance:    ()           => req('GET', '/api/points/balance'),
    getPointHistories:  (page = 0)   => req('GET', `/api/points/histories?page=${page}&size=10`),

    // PortOne 설정
    getPortOneConfig: () => req('GET', '/api/portone/config'),
  };
})();
