package com.billiard.service;

import com.billiard.common.CueRecommendConstants;
import com.billiard.dto.CueRecommendRequest;
import com.billiard.dto.CueRecommendResult;
import com.billiard.entity.Product;
import com.billiard.entity.ProductPrice;
import com.billiard.mapper.ProductMapper;
import com.billiard.mapper.ProductPriceMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 台球杆离线规则推荐引擎 - 单元测试（JUnit 5 标准格式）
 * - 运行方式1：maven 构建自动执行：mvn test
 * - 运行方式2：IDE 右键类名 -> Run Test，或手动执行 main 方法查看彩色输出
 * - 覆盖：杆型映射（cue_type）、硬度映射、推荐理由模板、四层兜底容错逻辑
 * - 说明：杆型复用 product.cue_type 字段，价位复用 product_price 表（区间过滤），
 *         不再使用冗余的 rod_type / price_level 字段。
 * - 纯逻辑测试，不依赖 Spring 容器和数据库（用 java.lang.reflect.Proxy mock Mapper）
 */
@TestInstance(Lifecycle.PER_CLASS)
public class CueRecommendRuleEngineTest {

    private int passed = 0;
    private int failed = 0;
    private CueRecommendRuleEngine engine;

    @BeforeAll
    void setUp() throws Exception {
        engine = createEngine();
        System.out.println("========== 台球杆离线规则推荐引擎 单元测试（JUnit 5）==========\n");
    }

    @AfterAll
    void tearDown() {
        System.out.println("\n========== 测试结果 ==========");
        System.out.println("通过: " + passed + " | 失败: " + failed);
        if (failed > 0) {
            System.out.println("⚠️  存在失败的测试，请检查！");
            fail("存在失败的测试用例，共 " + failed + " 项未通过");
        } else {
            System.out.println("✅ 所有测试通过！");
        }
    }

    /** 兼容 IDE 手动运行 main：顺序调用 6 个测试方法，输出和 mvn test 一致 */
    public static void main(String[] args) throws Exception {
        CueRecommendRuleEngineTest t = new CueRecommendRuleEngineTest();
        t.setUp();
        try {
            t.testMapCueTypes();
            t.testMapHardness();
            t.testBuildReason();
            t.testFullMatchScenario();
            t.testFallbackScenarios();
            t.testEdgeCases();
        } finally {
            t.tearDown();
        }
    }

    // ========== 第一层：玩法→杆型映射测试（cue_type）==========
    @Test
    @DisplayName("杆型映射：playType → cue_types（追分走位=大头杆；准度/斯诺克/均衡=小头杆）")
    void testMapCueTypes() throws Exception {
        System.out.println("\n--- 测试1：杆型映射 mapCueTypes ---");
        Method m = CueRecommendRuleEngine.class.getDeclaredMethod("mapCueTypes", String.class);
        m.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> offense = (List<String>) m.invoke(engine, CueRecommendConstants.PLAY_OFFENSE);
        assertEqual("追分走位 → 1种杆型", 1, offense.size());
        assertEqual("追分走位 → 大头杆", CueRecommendConstants.ROD_LARGE, offense.get(0));

        @SuppressWarnings("unchecked")
        List<String> defense = (List<String>) m.invoke(engine, CueRecommendConstants.PLAY_DEFENSE);
        assertEqual("准度防守 → 1种杆型", 1, defense.size());
        assertEqual("准度防守 → 小头杆", CueRecommendConstants.ROD_SMALL, defense.get(0));

        @SuppressWarnings("unchecked")
        List<String> snooker = (List<String>) m.invoke(engine, CueRecommendConstants.PLAY_SNOOKER);
        assertEqual("斯诺克玩法 → 1种杆型", 1, snooker.size());
        assertEqual("斯诺克玩法 → 小头杆", CueRecommendConstants.ROD_SMALL, snooker.get(0));

        @SuppressWarnings("unchecked")
        List<String> balance = (List<String>) m.invoke(engine, CueRecommendConstants.PLAY_BALANCE);
        assertEqual("均衡打法 → 小头杆", CueRecommendConstants.ROD_SMALL, balance.get(0));

        @SuppressWarnings("unchecked")
        List<String> nullType = (List<String>) m.invoke(engine, (Object) null);
        assertEqual("null → 小头杆", CueRecommendConstants.ROD_SMALL, nullType.get(0));
    }

    // ========== 第二层：硬度映射测试 ==========
    @Test
    @DisplayName("硬度映射：powerType + likeType + level → 目标硬度（含相邻硬度容错）")
    void testMapHardness() throws Exception {
        System.out.println("\n--- 测试2：硬度映射 mapHardness ---");
        Method m = CueRecommendRuleEngine.class.getDeclaredMethod("mapHardness", String.class, String.class, String.class);
        m.setAccessible(true);

        assertEqual("新手 → 中性", CueRecommendConstants.HARD_MEDIUM,
                m.invoke(engine, CueRecommendConstants.POWER_STRONG, CueRecommendConstants.LIKE_ACCURACY, CueRecommendConstants.LEVEL_BEGINNER));
        assertEqual("大力+准度 → 偏硬", CueRecommendConstants.HARD_HARD,
                m.invoke(engine, CueRecommendConstants.POWER_STRONG, CueRecommendConstants.LIKE_ACCURACY, CueRecommendConstants.LEVEL_INTERMEDIATE));
        assertEqual("轻推+走位 → 偏软", CueRecommendConstants.HARD_SOFT,
                m.invoke(engine, CueRecommendConstants.POWER_LIGHT, CueRecommendConstants.LIKE_POSITION, CueRecommendConstants.LEVEL_INTERMEDIATE));
        assertEqual("均匀+攻守 → 中性", CueRecommendConstants.HARD_MEDIUM,
                m.invoke(engine, CueRecommendConstants.POWER_MEDIUM, CueRecommendConstants.LIKE_BALANCE, CueRecommendConstants.LEVEL_ADVANCED));
        assertEqual("大力+走位 → 中性(默认)", CueRecommendConstants.HARD_MEDIUM,
                m.invoke(engine, CueRecommendConstants.POWER_STRONG, CueRecommendConstants.LIKE_POSITION, CueRecommendConstants.LEVEL_INTERMEDIATE));
        assertEqual("null参数 → 中性", CueRecommendConstants.HARD_MEDIUM,
                m.invoke(engine, (Object) null, (Object) null, (Object) null));
    }

    // ========== 推荐理由模板测试 ==========
    @Test
    @DisplayName("推荐理由模板：完全匹配/放宽匹配/仅预算/兜底 四种文案是否完整")
    void testBuildReason() throws Exception {
        System.out.println("\n--- 测试3：推荐理由模板 buildReason ---");
        Method m = CueRecommendRuleEngine.class.getDeclaredMethod("buildReason",
                Product.class, ProductPrice.class, List.class, String.class, int.class, CueRecommendRequest.class);
        m.setAccessible(true);

        Product p = new Product();
        p.setName("LP尊者S");
        p.setCueType(CueRecommendConstants.ROD_SMALL);
        p.setHardness(CueRecommendConstants.HARD_MEDIUM);
        p.setMaterial(CueRecommendConstants.MAT_WOOD);
        p.setFitPower(CueRecommendConstants.POWER_MEDIUM);
        p.setFitPlay(CueRecommendConstants.PLAY_DEFENSE);

        CueRecommendRequest req = new CueRecommendRequest();
        req.setPlayType(CueRecommendConstants.PLAY_DEFENSE);
        req.setLevel(CueRecommendConstants.LEVEL_INTERMEDIATE);
        req.setPowerType(CueRecommendConstants.POWER_MEDIUM);
        req.setLikeType(CueRecommendConstants.LIKE_BALANCE);
        req.setMinPrice(500.0);
        req.setMaxPrice(1500.0);

        ProductPrice pp = new ProductPrice();
        pp.setProductId(1L);
        pp.setPlatformPrice(new java.math.BigDecimal("800"));

        List<String> cueTypes = Arrays.asList(CueRecommendConstants.ROD_SMALL);

        String reason = (String) m.invoke(engine, p, pp, cueTypes, CueRecommendConstants.HARD_MEDIUM,
                CueRecommendConstants.MATCH_FULL, req);
        System.out.println("  生成理由(完全匹配): " + reason);
        assertContains("理由含玩法", reason, CueRecommendConstants.PLAY_DEFENSE);
        assertContains("理由含杆型", reason, CueRecommendConstants.ROD_SMALL);
        assertContains("理由含硬度", reason, CueRecommendConstants.HARD_MEDIUM);
        assertContains("理由含材质", reason, CueRecommendConstants.MAT_WOOD);
        assertContains("理由含平台价", reason, "平台价");
        assertContains("理由含发力", reason, CueRecommendConstants.POWER_MEDIUM);
        assertContains("理由含打法", reason, CueRecommendConstants.PLAY_DEFENSE);
        assertContains("理由含完全匹配", reason, "完全匹配");
        assertContains("理由含预算区间", reason, "符合您的预算区间");

        String relaxedReason = (String) m.invoke(engine, p, pp, cueTypes, CueRecommendConstants.HARD_SOFT,
                CueRecommendConstants.MATCH_RELAXED, req);
        System.out.println("  生成理由(放宽硬度): " + relaxedReason);
        assertContains("放宽匹配含推荐", relaxedReason, "推荐");

        String budgetReason = (String) m.invoke(engine, p, pp, cueTypes, null,
                CueRecommendConstants.MATCH_BUDGET_ONLY, req);
        System.out.println("  生成理由(仅预算): " + budgetReason);
        assertContains("仅预算含精选", budgetReason, "精选");

        String randomReason = (String) m.invoke(engine, p, pp, cueTypes, null,
                CueRecommendConstants.MATCH_RANDOM, req);
        System.out.println("  生成理由(兜底随机): " + randomReason);
        assertContains("兜底含供您参考", randomReason, "供您参考");
    }

    // ========== 完整匹配场景测试 ==========
    @Test
    @DisplayName("完整场景：准度防守+中级+均匀+攻守均衡+预算500-1500，返回结果结构正确")
    void testFullMatchScenario() {
        System.out.println("\n--- 测试4：完整匹配场景 ---");
        CueRecommendRequest req = new CueRecommendRequest();
        req.setPlayType(CueRecommendConstants.PLAY_DEFENSE);
        req.setLevel(CueRecommendConstants.LEVEL_INTERMEDIATE);
        req.setPowerType(CueRecommendConstants.POWER_MEDIUM);
        req.setLikeType(CueRecommendConstants.LIKE_BALANCE);
        req.setMinPrice(500.0);
        req.setMaxPrice(1500.0);

        List<CueRecommendResult> results = engine.recommend(req);
        System.out.println("  准度防守+预算500-1500 → 返回" + results.size() + "条结果");

        for (CueRecommendResult r : results) {
            check("结果有productId", r.getProductId() != null);
            check("结果有productName", r.getProductName() != null && !r.getProductName().isEmpty());
            check("结果有reason", r.getReason() != null && !r.getReason().isEmpty());
            check("结果matchLevel合法", r.getMatchLevel() >= 1 && r.getMatchLevel() <= 4);
        }
    }

    // ========== 兜底容错测试（四层兜底）==========
    @Test
    @DisplayName("兜底容错：超高预算/超低预算/斯诺克玩法，均最多返回3条，不抛异常")
    void testFallbackScenarios() {
        System.out.println("\n--- 测试5：兜底容错场景 ---");

        CueRecommendRequest req1 = new CueRecommendRequest();
        req1.setPlayType(CueRecommendConstants.PLAY_OFFENSE);
        req1.setLevel(CueRecommendConstants.LEVEL_ADVANCED);
        req1.setPowerType(CueRecommendConstants.POWER_STRONG);
        req1.setLikeType(CueRecommendConstants.LIKE_ACCURACY);
        req1.setMinPrice(50000.0);
        req1.setMaxPrice(100000.0);
        List<CueRecommendResult> results1 = engine.recommend(req1);
        System.out.println("  超高预算+追分走位 → 返回" + results1.size() + "条");
        check("超高预算最多3条", results1.size() <= 3);

        CueRecommendRequest req2 = new CueRecommendRequest();
        req2.setPlayType(CueRecommendConstants.PLAY_BALANCE);
        req2.setLevel(CueRecommendConstants.LEVEL_BEGINNER);
        req2.setPowerType(CueRecommendConstants.POWER_LIGHT);
        req2.setLikeType(CueRecommendConstants.LIKE_POSITION);
        req2.setMinPrice(50.0);
        req2.setMaxPrice(100.0);
        List<CueRecommendResult> results2 = engine.recommend(req2);
        System.out.println("  超低预算+新手 → 返回" + results2.size() + "条");
        check("超低预算最多3条", results2.size() <= 3);

        CueRecommendRequest req3 = new CueRecommendRequest();
        req3.setPlayType(CueRecommendConstants.PLAY_SNOOKER);
        req3.setLevel(CueRecommendConstants.LEVEL_ADVANCED);
        req3.setPowerType(CueRecommendConstants.POWER_STRONG);
        req3.setLikeType(CueRecommendConstants.LIKE_ACCURACY);
        req3.setMinPrice(2000.0);
        req3.setMaxPrice(3000.0);
        List<CueRecommendResult> results3 = engine.recommend(req3);
        System.out.println("  斯诺克玩法+预算2000-3000 → 返回" + results3.size() + "条");
        check("斯诺克最多3条", results3.size() <= 3);
    }

    // ========== 边界条件测试 ==========
    @Test
    @DisplayName("边界条件：全 null 请求，返回空列表（打分制规则：预算硬过滤，不兜底）")
    void testEdgeCases() {
        System.out.println("\n--- 测试6：边界条件 ---");
        CueRecommendRequest req = new CueRecommendRequest();
        List<CueRecommendResult> results = engine.recommend(req);
        System.out.println("  全null请求 → 返回" + results.size() + "条");
        assertNotNull(results, "全null请求不抛异常");
        check("全null请求列表非空", results != null);

        for (CueRecommendResult r : results) {
            check("结果有productId", r.getProductId() != null);
            check("结果有productName", r.getProductName() != null);
            check("结果有matchLevel", r.getMatchLevel() > 0);
            check("结果有reason", r.getReason() != null);
        }
    }

    // ========== 辅助方法 ==========

    private CueRecommendRuleEngine createEngine() throws Exception {
        CueRecommendRuleEngine e = new CueRecommendRuleEngine();

        ProductMapper mockProductMapper = (ProductMapper) java.lang.reflect.Proxy.newProxyInstance(
                ProductMapper.class.getClassLoader(),
                new Class[]{ProductMapper.class},
                (proxy, method, args) -> {
                    if ("selectList".equals(method.getName())) return Collections.emptyList();
                    if ("selectCount".equals(method.getName())) return 0L;
                    if ("selectById".equals(method.getName())) return null;
                    if ("updateById".equals(method.getName()) || "insert".equals(method.getName())) return 0;
                    return null;
                }
        );

        ProductPriceMapper mockPriceMapper = (ProductPriceMapper) java.lang.reflect.Proxy.newProxyInstance(
                ProductPriceMapper.class.getClassLoader(),
                new Class[]{ProductPriceMapper.class},
                (proxy, method, args) -> {
                    if ("selectOne".equals(method.getName())) {
                        ProductPrice pp = new ProductPrice();
                        pp.setProductId(1L);
                        pp.setPlatformPrice(new java.math.BigDecimal("800"));
                        return pp;
                    }
                    if ("selectList".equals(method.getName())) return Collections.emptyList();
                    return null;
                }
        );

        setField(e, "productMapper", mockProductMapper);
        setField(e, "productPriceMapper", mockPriceMapper);
        setField(e, "baseUrl", "http://localhost:8080/billiard_img/");
        return e;
    }

    private static void setField(Object obj, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field f = obj.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(obj, value);
    }

    // ========== 断言工具 ==========

    private void assertEqual(String label, Object expected, Object actual) {
        boolean pass = expected == null ? actual == null : expected.equals(actual);
        if (pass) {
            passed++;
            System.out.println("  ✅ " + label + " → " + actual);
        } else {
            failed++;
            System.out.println("  ❌ " + label + " → 期望[" + expected + "] 实际[" + actual + "]");
        }
    }

    private void check(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  ✅ " + label);
        } else {
            failed++;
            System.out.println("  ❌ " + label);
        }
    }

    private void assertContains(String label, String text, String keyword) {
        boolean pass = text != null && text.contains(keyword);
        if (pass) {
            passed++;
            System.out.println("  ✅ " + label);
        } else {
            failed++;
            System.out.println("  ❌ " + label + " → 文本[" + text + "] 不包含[" + keyword + "]");
        }
    }
}
