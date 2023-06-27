

package org.jetbrains.kotlin.fir.dataframe;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.fir.dataframe.GenerateTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("testData/box")
@TestDataPath("$PROJECT_ROOT")
public class DataFrameBlackBoxCodegenTestGenerated extends AbstractDataFrameBlackBoxCodegenTest {
    @Test
    public void testAllFilesPresentInBox() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("testData/box"), Pattern.compile("^(.+)\\.kt$"), null, true);
    }

    @Test
    @TestMetadata("conflictingJvmDeclarations.kt")
    public void testConflictingJvmDeclarations() throws Exception {
        runTest("testData/box/conflictingJvmDeclarations.kt");
    }

    @Test
    @TestMetadata("diff.kt")
    public void testDiff() throws Exception {
        runTest("testData/box/diff.kt");
    }

    @Test
    @TestMetadata("duplicatedSignature.kt")
    public void testDuplicatedSignature() throws Exception {
        runTest("testData/box/duplicatedSignature.kt");
    }

    @Test
    @TestMetadata("explodeDataFrame.kt")
    public void testExplodeDataFrame() throws Exception {
        runTest("testData/box/explodeDataFrame.kt");
    }

    @Test
    @TestMetadata("extractPluginSchemaWithUnfold.kt")
    public void testExtractPluginSchemaWithUnfold() throws Exception {
        runTest("testData/box/extractPluginSchemaWithUnfold.kt");
    }

    @Test
    @TestMetadata("flexibleReturnType.kt")
    public void testFlexibleReturnType() throws Exception {
        runTest("testData/box/flexibleReturnType.kt");
    }

    @Test
    @TestMetadata("injectAccessorsClassScope.kt")
    public void testInjectAccessorsClassScope() throws Exception {
        runTest("testData/box/injectAccessorsClassScope.kt");
    }

    @Test
    @TestMetadata("join.kt")
    public void testJoin() throws Exception {
        runTest("testData/box/join.kt");
    }

    @Test
    @TestMetadata("lowerGeneratedImplicitReceiver.kt")
    public void testLowerGeneratedImplicitReceiver() throws Exception {
        runTest("testData/box/lowerGeneratedImplicitReceiver.kt");
    }

    @Test
    @TestMetadata("OuterClass.kt")
    public void testOuterClass() throws Exception {
        runTest("testData/box/OuterClass.kt");
    }

    @Test
    @TestMetadata("platformType.kt")
    public void testPlatformType() throws Exception {
        runTest("testData/box/platformType.kt");
    }

    @Test
    @TestMetadata("readCSV.kt")
    public void testReadCSV() throws Exception {
        runTest("testData/box/readCSV.kt");
    }

    @Test
    @TestMetadata("readJson.kt")
    public void testReadJson() throws Exception {
        runTest("testData/box/readJson.kt");
    }

    @Test
    @TestMetadata("unhandledIntrisic.kt")
    public void testUnhandledIntrisic() throws Exception {
        runTest("testData/box/unhandledIntrisic.kt");
    }
}
