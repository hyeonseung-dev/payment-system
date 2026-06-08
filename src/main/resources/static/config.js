/**
 * =====================================================
 *  SPARTA SHOP : 결제 테스트 페이지 공통 설정
 * =====================================================
 *
 *  일반 결제 테스트에 필요한 PortOne 공개 설정을 서버에서 조회한다.
 *  구독 결제는 과제 범위에 포함하지 않는다.
 */

window.CONFIG = {
  /**
   * PortOne 일반 결제 설정을 조회한다.
   *
   * @returns {Promise<{storeId: string, channelKey: string}>} PortOne 결제창 공개 설정
   */
  async load() {
    const response = await fetch("/api/portone/config");
    const body = await response.json();

    if (!response.ok || !body.data) {
      throw new Error("PortOne 설정을 불러오지 못했습니다.");
    }

    return {
      storeId: body.data.storeId,
      channelKey: body.data.channelKey
    };
  }
};
