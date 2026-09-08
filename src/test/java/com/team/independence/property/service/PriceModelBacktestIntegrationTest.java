package com.team.independence.property.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.team.independence.config.RootConfig;
import com.team.independence.goal.service.MonteCarloEngine;
import com.team.independence.property.domain.HousingType;
import com.team.independence.property.dto.BacktestComboRow;
import com.team.independence.property.dto.MonthlyPricePoint;
import com.team.independence.property.mapper.RentTransactionMapper;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * PriceModel GBM 예측의 walk-forward 백테스팅.
 *
 * <p>실행 순서:
 * <ol>
 *   <li>docker compose up -d</li>
 *   <li>@Disabled 제거</li>
 *   <li>backfill_대표지역_36개월 실행 (MOLIT API 1,764회 호출 — 약 60분 소요)</li>
 *   <li>walkForward_1년·2년_예측_커버리지 실행 후 콘솔에서 커버리지 확인</li>
 * </ol>
 *
 * <p>사전 조건: docker compose up -d, 환경변수 MOLIT_SERVICE_KEY
 *
 * <p>백테스팅 방법 (walk-forward):
 * <pre>
 *   train: medians[0 .. cutoff-1]  →  P(0) = medians[cutoff-1]
 *   GBM Monte Carlo T개월 예측      →  [P5, P95]
 *   actual: medians[cutoff + T - 1]
 *   covered = actual ∈ [P5, P95]
 * </pre>
 * DB에 적재된 모든 (지역, 주거유형, 거래유형) 조합 × 5개 평수 버킷을 자동 탐색하므로
 * backfill 대상을 늘릴수록 검증 범위가 넓어진다.
 *
 * <p>합격 기준:
 * <ul>
 *   <li>1년(T=12) 커버리지 ≥ 70% (이론 90%CI 대비 시장 이상 변동 여유분 20%p)</li>
 *   <li>2년(T=24) 커버리지 ≥ 65% (장기 불확실성 반영 추가 완화)</li>
 * </ul>
 */
@Disabled("로컬 MySQL + MOLIT API 환경 전용")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PriceModelBacktestIntegrationTest {

    /** RecommendationAlgorithm.SIZE_BUCKETS와 동일 (평 단위) */
    private static final int[][] SIZE_BUCKETS = {{26, 40}, {20, 25}, {15, 19}, {10, 14}, {4, 9}};
    private static final double PYEONG_TO_SQM    = 3.305785;
    private static final int    MIN_SAMPLES_PER_MONTH = 5;
    private static final int    MIN_TRAIN_MONTHS = 6;   // PriceModel.estimate 최소 요건
    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    /**
     * 대표 49개 지역 코드:
     * 서울 25구 + 부산 주요 6구 + 대구 주요 3구 + 인천 주요 3구
     *           + 대전 2구 + 경기 핵심 5곳 + 기타 5곳
     *
     * <p>지역이 많을수록 검증 신뢰도가 높아진다. 필요하면 추가 가능.
     */
    private static final List<String> BACKFILL_REGIONS = List.of(
            // 서울 25구
            "11110","11140","11170","11200","11215","11230","11260","11290",
            "11305","11320","11350","11380","11410","11440","11470","11500",
            "11530","11545","11560","11590","11620","11650","11680","11710","11740",
            // 부산 주요 6구 (부산진·동래·남구·해운대·연제·수영)
            "26230","26260","26290","26350","26470","26500",
            // 대구 주요 3구 (북구·수성·달서)
            "27230","27260","27290",
            // 인천 주요 3구 (연수·남동·부평)
            "28185","28200","28237",
            // 대전 주요 2구 (서구·유성)
            "30170","30200",
            // 경기 핵심 5곳 (수원영통·성남분당·안양동안·고양일산동·용인수지)
            "41117","41135","41173","41285","41465",
            // 기타 5곳 (창원·김해·춘천·원주·청주흥덕)
            "48120","48250","51110","51130","43113"
    );

    @Autowired private RentTransactionSyncService rentTransactionSyncService;
    @Autowired private RentTransactionMapper      rentTransactionMapper;

    /**
     * 대표 49개 지역 × APT × 36개월 데이터를 MOLIT API에서 받아 적재한다.
     *
     * <p>API 호출: 49 × 36 = 1,764회. 예상 소요: 약 60분.
     * 이미 적재된 달은 재적재(기존 행 삭제 후 삽입)한다.
     */
    @Test
    @Order(1)
    void backfill_대표지역_36개월() {
        YearMonth base  = YearMonth.now().minusMonths(1);
        int total = BACKFILL_REGIONS.size() * 36;
        int done  = 0;
        for (String regionCode : BACKFILL_REGIONS) {
            for (int i = 0; i < 36; i++) {
                String dealYm = base.minusMonths(i).format(YM_FMT);
                rentTransactionSyncService.collectAndSync(regionCode, dealYm, HousingType.APT);
                done++;
            }
            System.out.printf("[%d/%d] %s 완료%n", done, total, regionCode);
        }
        System.out.println("백필 완료.");
    }

    /**
     * Walk-forward 백테스팅 — T=12개월 예측 커버리지.
     *
     * <p>DB에 적재된 전체 (지역, 주거유형, 거래유형) 조합을 자동 탐색하여 1년 후 가격을 예측한다.
     * 합격 기준: 실제 중앙값이 MC [P5, P95] 구간 안에 드는 비율 ≥ 70%.
     */
    @Test
    @Order(2)
    void walkForward_1년_예측_커버리지() {
        runWalkForward(12, 0.70, "1년(T=12)");
    }

    /**
     * Walk-forward 백테스팅 — T=24개월 예측 커버리지.
     *
     * <p>2년 예측은 불확실성이 크므로 합격 기준을 ≥ 65%로 완화한다.
     */
    @Test
    @Order(3)
    void walkForward_2년_예측_커버리지() {
        runWalkForward(24, 0.65, "2년(T=24)");
    }

    private void runWalkForward(int horizonMonths, double minCoverage, String label) {
        YearMonth end   = YearMonth.now().minusMonths(1);
        YearMonth start = end.minusMonths(35);  // 36개월 창
        String startYm  = start.format(YM_FMT);
        String endYm    = end.format(YM_FMT);

        // SQL 단계: horizonMonths만큼 예측하려면 최소 (MIN_TRAIN_MONTHS + horizonMonths) 월 필요
        int minMonths = MIN_TRAIN_MONTHS + horizonMonths;
        List<BacktestComboRow> combos =
                rentTransactionMapper.findDistinctCombosForBacktest(startYm, endYm, minMonths);

        if (combos.isEmpty()) {
            System.out.printf("[WARN][%s] 백테스팅 가능한 조합 없음. backfill 먼저 실행하세요.%n", label);
            return;
        }

        int totalFolds   = 0;
        int coveredFolds = 0;
        int skippedBuckets = 0;
        Map<String, int[]> perRegion = new LinkedHashMap<>();

        for (BacktestComboRow combo : combos) {
            for (int[] bucket : SIZE_BUCKETS) {
                long areaMinSqm = (long) Math.floor(bucket[0] * PYEONG_TO_SQM);
                long areaMaxSqm = (long) Math.floor(bucket[1] * PYEONG_TO_SQM);

                List<MonthlyPricePoint> raw = rentTransactionMapper.findAmountsForPriceModel(
                        combo.getRegionCode(), combo.getHousingType(), combo.getDealType(),
                        areaMinSqm, areaMaxSqm, startYm, endYm);

                List<Double> medians = monthlyMedians(raw);
                if (medians.size() < minMonths) {
                    skippedBuckets++;
                    continue;
                }

                // walk-forward 폴드 순회
                // P(0) = medians[cutoff-1],  actual = medians[cutoff + horizonMonths - 1]
                for (int cutoff = MIN_TRAIN_MONTHS;
                         cutoff <= medians.size() - horizonMonths;
                         cutoff++) {

                    List<Double> train  = medians.subList(0, cutoff);
                    double actual       = medians.get(cutoff + horizonMonths - 1);

                    PriceModel model = PriceModel.estimate(train);
                    long p0          = Math.round(train.get(cutoff - 1));

                    MonteCarloEngine.Result mc = MonteCarloEngine.simulate(
                            model.annualDrift(), model.annualVol(),
                            p0, Long.MAX_VALUE,
                            horizonMonths,
                            MonteCarloEngine.DEFAULT_SIMULATIONS,
                            MonteCarloEngine.DEFAULT_SEED);

                    boolean covered = actual >= mc.priceP5() && actual <= mc.priceP95();
                    totalFolds++;
                    if (covered) coveredFolds++;

                    int[] stat = perRegion.computeIfAbsent(combo.getRegionCode(), k -> new int[2]);
                    stat[0]++;
                    if (covered) stat[1]++;
                }
            }
        }

        // 결과 출력
        System.out.printf("%n=== Walk-forward 백테스팅 결과 [%s] ===%n", label);
        System.out.printf("탐색된 조합: %d개  스킵 버킷: %d  총 폴드: %d%n",
                combos.size(), skippedBuckets, totalFolds);

        if (totalFolds == 0) {
            System.out.println("[WARN] 유효 폴드 없음. 더 많은 데이터 적재 후 재시도하세요.");
            return;
        }

        double coverage = (double) coveredFolds / totalFolds;
        System.out.printf("전체 커버리지: %.1f%%  (%d/%d covered)%n",
                coverage * 100, coveredFolds, totalFolds);

        System.out.println("\n[커버리지 하위 5개 지역]");
        perRegion.entrySet().stream()
                .filter(e -> e.getValue()[0] > 0)
                .sorted(Comparator.comparingDouble(e -> (double) e.getValue()[1] / e.getValue()[0]))
                .limit(5)
                .forEach(e -> System.out.printf("  %-6s  %.1f%%  (%d/%d)%n",
                        e.getKey(),
                        100.0 * e.getValue()[1] / e.getValue()[0],
                        e.getValue()[1], e.getValue()[0]));

        System.out.println("[커버리지 상위 5개 지역]");
        perRegion.entrySet().stream()
                .filter(e -> e.getValue()[0] > 0)
                .sorted(Comparator.<Map.Entry<String, int[]>>comparingDouble(
                        e -> (double) e.getValue()[1] / e.getValue()[0]).reversed())
                .limit(5)
                .forEach(e -> System.out.printf("  %-6s  %.1f%%  (%d/%d)%n",
                        e.getKey(),
                        100.0 * e.getValue()[1] / e.getValue()[0],
                        e.getValue()[1], e.getValue()[0]));

        assertTrue(coverage >= minCoverage, String.format(
                "[%s] 커버리지 %.1f%%가 합격 기준 %.0f%%를 하회합니다. 모델 검토 필요.",
                label, coverage * 100, minCoverage * 100));
    }

    /** raw MonthlyPricePoint 목록에서 월별 중앙값(원/평) 시계열을 만든다. */
    private List<Double> monthlyMedians(List<MonthlyPricePoint> raw) {
        return raw.stream()
                .collect(Collectors.groupingBy(MonthlyPricePoint::getDealYm))
                .entrySet().stream()
                .filter(e -> e.getValue().size() >= MIN_SAMPLES_PER_MONTH)
                .sorted(Map.Entry.comparingByKey())
                .map(e -> medianPerPyeong(e.getValue()))
                .collect(Collectors.toList());
    }

    private double medianPerPyeong(List<MonthlyPricePoint> points) {
        double[] sorted = points.stream()
                .mapToDouble(p -> p.getDeposit() / (p.getArea().doubleValue() / PYEONG_TO_SQM))
                .sorted()
                .toArray();
        int mid = sorted.length / 2;
        return sorted.length % 2 == 0
                ? (sorted[mid - 1] + sorted[mid]) / 2.0
                : sorted[mid];
    }
}
