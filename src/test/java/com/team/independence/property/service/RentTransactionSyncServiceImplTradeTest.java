package com.team.independence.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team.independence.property.domain.DealType;
import com.team.independence.property.domain.HousingType;
import com.team.independence.property.domain.RentTransaction;
import com.team.independence.property.mapper.RegionMapper;
import com.team.independence.property.mapper.RentSyncLogMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

/**
 * 매매(TRADE) 수집 변환 로직 단위 테스트.
 *
 * <p>RestTemplate을 목 처리해 국토부 매매 API 응답을 가짜 XML로 대체하고,
 * toRentTransaction()이 deposit = dealAmount × 10,000, monthly_rent = 0,
 * deal_type = TRADE로 올바르게 변환하는지 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class RentTransactionSyncServiceImplTradeTest {

    private static final String REGION_CODE = "11680";
    private static final String DEAL_YM     = "202406";

    /** dealAmount = 85,000(만원) → deposit = 850,000,000(원) */
    private static final long EXPECTED_DEPOSIT = 85_000L * 10_000;

    private static final String TRADE_XML = String.join("\n",
        "<response>",
        "  <header><resultCode>000</resultCode><resultMsg>OK</resultMsg></header>",
        "  <body>",
        "    <items>",
        "      <item>",
        "        <dealYear>2024</dealYear>",
        "        <dealMonth>6</dealMonth>",
        "        <dealDay>15</dealDay>",
        "        <dealAmount>85,000</dealAmount>",
        "        <excluUseAr>84.99</excluUseAr>",
        "        <floor>5</floor>",
        "        <buildYear>2010</buildYear>",
        "        <aptNm>래미안강남</aptNm>",
        "        <sggCd>11680</sggCd>",
        "        <umdNm>역삼동</umdNm>",
        "        <jibun>123-4</jibun>",
        "      </item>",
        "    </items>",
        "    <totalCount>1</totalCount>",
        "  </body>",
        "</response>"
    );

    @Mock private RestTemplate restTemplate;
    @Mock private RegionMapper regionMapper;
    @Mock private RentSyncLogMapper rentSyncLogMapper;
    @Mock private RentTransactionUnitSyncService unitSyncService;

    @Captor private ArgumentCaptor<List<RentTransaction>> itemsCaptor;

    private RentTransactionSyncServiceImpl syncService;

    @BeforeEach
    void setUp() {
        syncService = new RentTransactionSyncServiceImpl(
                restTemplate, regionMapper, rentSyncLogMapper, unitSyncService);
        ReflectionTestUtils.setField(syncService, "apiKey", "TEST_KEY");
    }

    @Test
    @DisplayName("매매 API 응답의 dealAmount가 deposit(원 단위)으로 변환되고 deal_type은 TRADE다")
    void collectAndSyncTrade_dealAmount를_deposit으로_변환() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(TRADE_XML);

        syncService.collectAndSyncTrade(REGION_CODE, DEAL_YM, HousingType.APT);

        verify(unitSyncService).sync(
                eq(REGION_CODE), eq(DEAL_YM), eq(HousingType.APT),
                itemsCaptor.capture(), eq(true));

        List<RentTransaction> items = itemsCaptor.getValue();
        assertThat(items).hasSize(1);

        RentTransaction tx = items.get(0);
        assertThat(tx.getDealType()).isEqualTo(DealType.TRADE);
        assertThat(tx.getDeposit()).isEqualTo(EXPECTED_DEPOSIT);
        assertThat(tx.getMonthlyRent()).isZero();
    }

    @Test
    @DisplayName("매매 거래는 contractType, contractTerm이 null이다 (매매 API에 해당 필드 없음)")
    void collectAndSyncTrade_계약유형과_계약기간은_null() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(TRADE_XML);

        syncService.collectAndSyncTrade(REGION_CODE, DEAL_YM, HousingType.APT);

        verify(unitSyncService).sync(any(), any(), any(), itemsCaptor.capture(), eq(true));

        RentTransaction tx = itemsCaptor.getValue().get(0);
        assertThat(tx.getContractType()).isNull();
        assertThat(tx.getContractTerm()).isNull();
    }

    @Test
    @DisplayName("전월세 수집은 isTrade=false로 호출된다")
    void collectAndSync_전월세는_isTrade_false() {
        String rentXml = TRADE_XML
                .replace("<dealAmount>85,000</dealAmount>", "<deposit>30,000</deposit>")
                .replace("<dealAmount>85,000</dealAmount>", "")
                + "";

        String rentOnlyXml = String.join("\n",
            "<response>",
            "  <header><resultCode>000</resultCode><resultMsg>OK</resultMsg></header>",
            "  <body>",
            "    <items>",
            "      <item>",
            "        <dealYear>2024</dealYear>",
            "        <dealMonth>6</dealMonth>",
            "        <dealDay>15</dealDay>",
            "        <deposit>30,000</deposit>",
            "        <monthlyRent>0</monthlyRent>",
            "        <excluUseAr>84.99</excluUseAr>",
            "        <floor>5</floor>",
            "        <buildYear>2010</buildYear>",
            "        <aptNm>래미안강남</aptNm>",
            "        <sggCd>11680</sggCd>",
            "        <umdNm>역삼동</umdNm>",
            "        <jibun>123-4</jibun>",
            "      </item>",
            "    </items>",
            "    <totalCount>1</totalCount>",
            "  </body>",
            "</response>"
        );

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(rentOnlyXml);

        syncService.collectAndSync(REGION_CODE, DEAL_YM, HousingType.APT);

        verify(unitSyncService).sync(
                eq(REGION_CODE), eq(DEAL_YM), eq(HousingType.APT),
                itemsCaptor.capture(), eq(false));

        RentTransaction tx = itemsCaptor.getValue().get(0);
        assertThat(tx.getDealType()).isEqualTo(DealType.JEONSE);
        assertThat(tx.getDeposit()).isEqualTo(30_000L * 10_000);
    }
}
