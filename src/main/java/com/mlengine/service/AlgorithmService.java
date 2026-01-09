package com.mlengine.service;

import com.mlengine.model.dto.AlgorithmDTO;
import com.mlengine.model.enums.ProblemType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for Algorithm information.
 * Provides metadata for all 13+ supported algorithms.
 */
@Slf4j
@Service
public class AlgorithmService {

    private static final Map<String, AlgorithmDTO.Info> ALGORITHMS = new LinkedHashMap<>();
    private static final Map<String, AlgorithmDTO.Parameters> ALGORITHM_PARAMS = new HashMap<>();

    static {
        // Initialize all algorithms
        initializeAlgorithms();
        initializeParameters();
    }

    private static void initializeAlgorithms() {
        // ============ CLASSIFICATION & REGRESSION ============

        ALGORITHMS.put("logistic_regression", AlgorithmDTO.Info.builder()
                .id("logistic_regression")
                .name("Logistic Regression")
                .displayName("Logistic Regression")
                .description("Linear model for binary and multiclass classification")
                .supportedProblemTypes(List.of(ProblemType.CLASSIFICATION))
                .supportsGpu(false)
                .category("linear")
                .pros(List.of("Fast training", "Interpretable", "Works well with small datasets"))
                .cons(List.of("Limited to linear relationships", "May underfit complex data"))
                .recommendedFor("Baseline models, interpretable predictions")
                .build());

        ALGORITHMS.put("linear_regression", AlgorithmDTO.Info.builder()
                .id("linear_regression")
                .name("Linear Regression")
                .displayName("Linear Regression")
                .description("Simple linear model for regression tasks")
                .supportedProblemTypes(List.of(ProblemType.REGRESSION))
                .supportsGpu(false)
                .category("linear")
                .pros(List.of("Very fast", "Highly interpretable", "No hyperparameters"))
                .cons(List.of("Assumes linear relationships", "Sensitive to outliers"))
                .recommendedFor("Simple regression problems, baseline models")
                .build());

        ALGORITHMS.put("random_forest", AlgorithmDTO.Info.builder()
                .id("random_forest")
                .name("Random Forest")
                .displayName("Random Forest")
                .description("Ensemble of decision trees with bagging")
                .supportedProblemTypes(List.of(ProblemType.CLASSIFICATION, ProblemType.REGRESSION))
                .supportsGpu(false)
                .category("ensemble")
                .pros(List.of("Handles non-linear relationships", "Robust to outliers", "Feature importance"))
                .cons(List.of("Can be slow with many trees", "Less interpretable than single trees"))
                .recommendedFor("General purpose, feature importance analysis")
                .build());

        ALGORITHMS.put("xgboost", AlgorithmDTO.Info.builder()
                .id("xgboost")
                .name("XGBoost")
                .displayName("XGBoost (Gradient Boosting)")
                .description("Extreme Gradient Boosting - highly optimized gradient boosting")
                .supportedProblemTypes(List.of(ProblemType.CLASSIFICATION, ProblemType.REGRESSION))
                .supportsGpu(true)
                .category("ensemble")
                .pros(List.of("State-of-the-art performance", "Handles missing values", "GPU support"))
                .cons(List.of("Many hyperparameters", "Can overfit"))
                .recommendedFor("Competition-winning models, structured data")
                .build());

        ALGORITHMS.put("lightgbm", AlgorithmDTO.Info.builder()
                .id("lightgbm")
                .name("LightGBM")
                .displayName("LightGBM")
                .description("Light Gradient Boosting Machine - faster than XGBoost")
                .supportedProblemTypes(List.of(ProblemType.CLASSIFICATION, ProblemType.REGRESSION))
                .supportsGpu(true)
                .category("ensemble")
                .pros(List.of("Very fast training", "Low memory usage", "Handles large datasets"))
                .cons(List.of("Can overfit on small datasets", "Sensitive to hyperparameters"))
                .recommendedFor("Large datasets, when training speed matters")
                .build());

        ALGORITHMS.put("catboost", AlgorithmDTO.Info.builder()
                .id("catboost")
                .name("CatBoost")
                .displayName("CatBoost")
                .description("Gradient boosting with excellent categorical feature handling")
                .supportedProblemTypes(List.of(ProblemType.CLASSIFICATION, ProblemType.REGRESSION))
                .supportsGpu(true)
                .category("ensemble")
                .pros(List.of("Best for categorical features", "Robust to overfitting", "No preprocessing needed"))
                .cons(List.of("Slower than LightGBM", "Large model size"))
                .recommendedFor("Datasets with many categorical features")
                .build());

        ALGORITHMS.put("gradient_boosting", AlgorithmDTO.Info.builder()
                .id("gradient_boosting")
                .name("Gradient Boosting")
                .displayName("Gradient Boosting (sklearn)")
                .description("Scikit-learn's gradient boosting implementation")
                .supportedProblemTypes(List.of(ProblemType.CLASSIFICATION, ProblemType.REGRESSION))
                .supportsGpu(false)
                .category("ensemble")
                .pros(List.of("Good default performance", "Part of sklearn ecosystem"))
                .cons(List.of("Slower than XGBoost/LightGBM", "No GPU support"))
                .recommendedFor("When sklearn compatibility is needed")
                .build());

        ALGORITHMS.put("svm", AlgorithmDTO.Info.builder()
                .id("svm")
                .name("SVM")
                .displayName("Support Vector Machine")
                .description("Finds optimal hyperplane for classification")
                .supportedProblemTypes(List.of(ProblemType.CLASSIFICATION))
                .supportsGpu(false)
                .category("kernel")
                .pros(List.of("Effective in high dimensions", "Works with non-linear kernels"))
                .cons(List.of("Slow on large datasets", "Sensitive to feature scaling"))
                .recommendedFor("Small to medium datasets, text classification")
                .build());

        ALGORITHMS.put("knn", AlgorithmDTO.Info.builder()
                .id("knn")
                .name("KNN")
                .displayName("K-Nearest Neighbors")
                .description("Instance-based learning using nearest neighbors")
                .supportedProblemTypes(List.of(ProblemType.CLASSIFICATION, ProblemType.REGRESSION))
                .supportsGpu(false)
                .category("instance")
                .pros(List.of("Simple and intuitive", "No training phase", "Adapts to data"))
                .cons(List.of("Slow prediction on large datasets", "Sensitive to irrelevant features"))
                .recommendedFor("Small datasets, recommendation systems")
                .build());

        ALGORITHMS.put("naive_bayes", AlgorithmDTO.Info.builder()
                .id("naive_bayes")
                .name("Naive Bayes")
                .displayName("Naive Bayes")
                .description("Probabilistic classifier based on Bayes theorem")
                .supportedProblemTypes(List.of(ProblemType.CLASSIFICATION))
                .supportsGpu(false)
                .category("probabilistic")
                .pros(List.of("Very fast", "Works well with small data", "Good for text"))
                .cons(List.of("Assumes feature independence", "Can be outperformed by other methods"))
                .recommendedFor("Text classification, spam detection")
                .build());

        ALGORITHMS.put("decision_tree", AlgorithmDTO.Info.builder()
                .id("decision_tree")
                .name("Decision Tree")
                .displayName("Decision Tree")
                .description("Tree-based model with interpretable rules")
                .supportedProblemTypes(List.of(ProblemType.CLASSIFICATION, ProblemType.REGRESSION))
                .supportsGpu(false)
                .category("tree")
                .pros(List.of("Highly interpretable", "Handles non-linear relationships", "No scaling needed"))
                .cons(List.of("Prone to overfitting", "Unstable - small data changes affect structure"))
                .recommendedFor("When interpretability is critical")
                .build());

        ALGORITHMS.put("extra_trees", AlgorithmDTO.Info.builder()
                .id("extra_trees")
                .name("Extra Trees")
                .displayName("Extra Trees (Extremely Randomized Trees)")
                .description("Random forest variant with more randomization")
                .supportedProblemTypes(List.of(ProblemType.CLASSIFICATION, ProblemType.REGRESSION))
                .supportsGpu(false)
                .category("ensemble")
                .pros(List.of("Faster than random forest", "Less prone to overfitting"))
                .cons(List.of("Slightly lower accuracy than random forest"))
                .recommendedFor("When random forest is too slow")
                .build());

        ALGORITHMS.put("adaboost", AlgorithmDTO.Info.builder()
                .id("adaboost")
                .name("AdaBoost")
                .displayName("AdaBoost")
                .description("Adaptive boosting ensemble method")
                .supportedProblemTypes(List.of(ProblemType.CLASSIFICATION, ProblemType.REGRESSION))
                .supportsGpu(false)
                .category("ensemble")
                .pros(List.of("Less prone to overfitting", "Works well with weak learners"))
                .cons(List.of("Sensitive to noisy data and outliers"))
                .recommendedFor("Binary classification problems")
                .build());

        ALGORITHMS.put("neural_network", AlgorithmDTO.Info.builder()
                .id("neural_network")
                .name("Neural Network")
                .displayName("Neural Network (MLP)")
                .description("Multi-layer perceptron for complex patterns")
                .supportedProblemTypes(List.of(ProblemType.CLASSIFICATION, ProblemType.REGRESSION))
                .supportsGpu(true)
                .category("neural_network")
                .pros(List.of("Handles complex patterns", "Flexible architecture", "GPU acceleration"))
                .cons(List.of("Requires more data", "Many hyperparameters", "Black box"))
                .recommendedFor("Complex patterns, large datasets")
                .build());
    }

    private static void initializeParameters() {
        // XGBoost parameters
        ALGORITHM_PARAMS.put("xgboost", AlgorithmDTO.Parameters.builder()
                .algorithmId("xgboost")
                .algorithmName("XGBoost")
                .parameters(List.of(
                        AlgorithmDTO.Parameter.builder()
                                .name("max_depth").displayName("Max Depth")
                                .description("Maximum depth of trees")
                                .type("integer").defaultValue(6).minValue(1).maxValue(20).step(1.0)
                                .category("basic").required(false).build(),
                        AlgorithmDTO.Parameter.builder()
                                .name("learning_rate").displayName("Learning Rate")
                                .description("Step size shrinkage to prevent overfitting")
                                .type("float").defaultValue(0.1).minValue(0.01).maxValue(1.0).step(0.01)
                                .category("basic").required(false).build(),
                        AlgorithmDTO.Parameter.builder()
                                .name("n_estimators").displayName("Number of Estimators")
                                .description("Number of boosting rounds")
                                .type("integer").defaultValue(100).minValue(10).maxValue(1000).step(10.0)
                                .category("basic").required(false).build(),
                        AlgorithmDTO.Parameter.builder()
                                .name("min_child_weight").displayName("Min Child Weight")
                                .description("Minimum sum of instance weight needed in a child")
                                .type("integer").defaultValue(1).minValue(0).maxValue(10).step(1.0)
                                .category("advanced").required(false).build(),
                        AlgorithmDTO.Parameter.builder()
                                .name("subsample").displayName("Subsample")
                                .description("Subsample ratio of training instances")
                                .type("float").defaultValue(1.0).minValue(0.1).maxValue(1.0).step(0.1)
                                .category("advanced").required(false).build(),
                        AlgorithmDTO.Parameter.builder()
                                .name("colsample_bytree").displayName("Column Sample by Tree")
                                .description("Subsample ratio of columns for each tree")
                                .type("float").defaultValue(1.0).minValue(0.1).maxValue(1.0).step(0.1)
                                .category("advanced").required(false).build(),
                        AlgorithmDTO.Parameter.builder()
                                .name("random_state").displayName("Random State (Seed)")
                                .description("Random seed for reproducibility")
                                .type("integer").defaultValue(42).minValue(0).maxValue(999999).step(1.0)
                                .category("advanced").required(false).build()
                ))
                .recommended(AlgorithmDTO.RecommendedSettings.builder()
                        .problemType("classification")
                        .values(Map.of(
                                "learning_rate", 0.1,
                                "max_depth", 6,
                                "n_estimators", 100
                        ))
                        .tips(List.of(
                                "Learning rate: 0.1",
                                "Max depth: 6-8",
                                "Estimators: 100-300",
                                "Enable GPU acceleration"
                        ))
                        .build())
                .build());

        // Random Forest parameters
        ALGORITHM_PARAMS.put("random_forest", AlgorithmDTO.Parameters.builder()
                .algorithmId("random_forest")
                .algorithmName("Random Forest")
                .parameters(List.of(
                        AlgorithmDTO.Parameter.builder()
                                .name("n_estimators").displayName("Number of Estimators")
                                .description("Number of trees in the forest")
                                .type("integer").defaultValue(100).minValue(10).maxValue(1000).step(10.0)
                                .category("basic").required(false).build(),
                        AlgorithmDTO.Parameter.builder()
                                .name("max_depth").displayName("Max Depth")
                                .description("Maximum depth of trees (None for unlimited)")
                                .type("integer").defaultValue(10).minValue(1).maxValue(50).step(1.0)
                                .category("basic").required(false).build(),
                        AlgorithmDTO.Parameter.builder()
                                .name("min_samples_split").displayName("Min Samples Split")
                                .description("Minimum samples required to split a node")
                                .type("integer").defaultValue(2).minValue(2).maxValue(20).step(1.0)
                                .category("advanced").required(false).build(),
                        AlgorithmDTO.Parameter.builder()
                                .name("min_samples_leaf").displayName("Min Samples Leaf")
                                .description("Minimum samples required at a leaf node")
                                .type("integer").defaultValue(1).minValue(1).maxValue(20).step(1.0)
                                .category("advanced").required(false).build(),
                        AlgorithmDTO.Parameter.builder()
                                .name("max_features").displayName("Max Features")
                                .description("Number of features to consider for best split")
                                .type("select").defaultValue("sqrt")
                                .options(List.of("sqrt", "log2", "auto", "None"))
                                .category("advanced").required(false).build(),
                        AlgorithmDTO.Parameter.builder()
                                .name("random_state").displayName("Random State (Seed)")
                                .description("Random seed for reproducibility")
                                .type("integer").defaultValue(42).minValue(0).maxValue(999999).step(1.0)
                                .category("advanced").required(false).build()
                ))
                .recommended(AlgorithmDTO.RecommendedSettings.builder()
                        .problemType("classification")
                        .values(Map.of("n_estimators", 100, "max_depth", 10))
                        .tips(List.of("Start with 100 trees", "Increase if underfitting"))
                        .build())
                .build());

        // Add more algorithm parameters as needed...
    }

    /**
     * Get all algorithms grouped by problem type.
     */
    public AlgorithmDTO.ListResponse getAllAlgorithms() {
        List<AlgorithmDTO.Info> classification = new ArrayList<>();
        List<AlgorithmDTO.Info> regression = new ArrayList<>();
        List<AlgorithmDTO.Info> timeSeries = new ArrayList<>();
        List<AlgorithmDTO.Info> clustering = new ArrayList<>();

        for (AlgorithmDTO.Info algo : ALGORITHMS.values()) {
            if (algo.getSupportedProblemTypes().contains(ProblemType.CLASSIFICATION)) {
                classification.add(algo);
            }
            if (algo.getSupportedProblemTypes().contains(ProblemType.REGRESSION)) {
                regression.add(algo);
            }
            if (algo.getSupportedProblemTypes().contains(ProblemType.TIME_SERIES)) {
                timeSeries.add(algo);
            }
            if (algo.getSupportedProblemTypes().contains(ProblemType.CLUSTERING)) {
                clustering.add(algo);
            }
        }

        return AlgorithmDTO.ListResponse.builder()
                .classification(classification)
                .regression(regression)
                .timeSeries(timeSeries)
                .clustering(clustering)
                .build();
    }

    /**
     * Get algorithm info by ID.
     */
    public AlgorithmDTO.Info getAlgorithm(String algorithmId) {
        AlgorithmDTO.Info algo = ALGORITHMS.get(algorithmId.toLowerCase());
        if (algo == null) {
            throw new IllegalArgumentException("Unknown algorithm: " + algorithmId);
        }
        return algo;
    }

    /**
     * Get algorithm parameters schema.
     */
    public AlgorithmDTO.Parameters getAlgorithmParameters(String algorithmId) {
        AlgorithmDTO.Parameters params = ALGORITHM_PARAMS.get(algorithmId.toLowerCase());
        if (params == null) {
            // Return basic parameters for algorithms without specific params
            return AlgorithmDTO.Parameters.builder()
                    .algorithmId(algorithmId)
                    .algorithmName(getAlgorithm(algorithmId).getName())
                    .parameters(List.of(
                            AlgorithmDTO.Parameter.builder()
                                    .name("random_state").displayName("Random State")
                                    .type("integer").defaultValue(42)
                                    .category("basic").required(false).build()
                    ))
                    .build();
        }
        return params;
    }

    /**
     * Get display name for algorithm.
     */
    public String getAlgorithmDisplayName(String algorithmId) {
        AlgorithmDTO.Info algo = ALGORITHMS.get(algorithmId.toLowerCase());
        return algo != null ? algo.getDisplayName() : algorithmId;
    }

    /**
     * Get list of all algorithm IDs.
     */
    public List<String> getAlgorithmIds() {
        return new ArrayList<>(ALGORITHMS.keySet());
    }
}
