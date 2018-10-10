package com.alibaba.android.arouter.compiler.processor;

import com.alibaba.android.arouter.compiler.utils.Consts;
import com.alibaba.android.arouter.compiler.utils.Logger;
import com.alibaba.android.arouter.compiler.utils.PathUtils;
import com.alibaba.android.arouter.compiler.utils.TypeUtils;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.enums.RouteType;
import com.alibaba.android.arouter.facade.model.MethodModel;
import com.alibaba.android.arouter.facade.model.ParamModel;
import com.alibaba.android.arouter.facade.model.RouteMeta;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static com.alibaba.android.arouter.compiler.utils.Consts.ACTIVITY;
import static com.alibaba.android.arouter.compiler.utils.Consts.ANNOTATION_TYPE_AUTOWIRED;
import static com.alibaba.android.arouter.compiler.utils.Consts.ANNOTATION_TYPE_ROUTE;
import static com.alibaba.android.arouter.compiler.utils.Consts.METHOD_CALLBACK_ANNOTATION;
import static com.alibaba.android.arouter.compiler.utils.Consts.FRAGMENT;
import static com.alibaba.android.arouter.compiler.utils.Consts.IPROVIDER_GROUP;
import static com.alibaba.android.arouter.compiler.utils.Consts.IROUTE_GROUP;
import static com.alibaba.android.arouter.compiler.utils.Consts.IROUTE_METHOD;
import static com.alibaba.android.arouter.compiler.utils.Consts.ITROUTE_ROOT;
import static com.alibaba.android.arouter.compiler.utils.Consts.KEY_MODULE_NAME;
import static com.alibaba.android.arouter.compiler.utils.Consts.METHOD;
import static com.alibaba.android.arouter.compiler.utils.Consts.METHOD_CALLBACK;
import static com.alibaba.android.arouter.compiler.utils.Consts.METHOD_LOAD_INTO;
import static com.alibaba.android.arouter.compiler.utils.Consts.NAME_OF_GROUP;
import static com.alibaba.android.arouter.compiler.utils.Consts.NAME_OF_PROVIDER;
import static com.alibaba.android.arouter.compiler.utils.Consts.NAME_OF_ROOT;
import static com.alibaba.android.arouter.compiler.utils.Consts.PACKAGE_OF_GENERATE_FILE;
import static com.alibaba.android.arouter.compiler.utils.Consts.SEPARATOR;
import static com.alibaba.android.arouter.compiler.utils.Consts.SERVICE;
import static com.alibaba.android.arouter.compiler.utils.Consts.WARNING_TIPS;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * A processor used for find route.
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/15 下午10:08
 */
@AutoService(Processor.class)
@SupportedOptions(KEY_MODULE_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({ANNOTATION_TYPE_ROUTE, ANNOTATION_TYPE_AUTOWIRED})
public class RouteProcessor extends AbstractProcessor {
    private Map<String, Set<RouteMeta>> groupMap = new HashMap<>(); // ModuleName and routeMeta.
    private Map<String, String> rootMap = new TreeMap<>();  // Map of root metas, used for generate class file in order.
    private Filer mFiler;       // File util, write class file into disk.
    private Logger logger;
    private Types types;
    private Elements elements;
    private TypeUtils typeUtils;
    private String moduleName = null;   // Module name, maybe its 'app' or others
    private TypeMirror iProvider = null;
    private Map<String, List<MethodModel>> methods = new HashMap<>(10);
    private Map<String, String> providerPaths = new HashMap<>(10);

    /**
     * Initializes the processor with the processing environment by
     * setting the {@code processingEnv} field to the value of the
     * {@code processingEnv} argument.  An {@code
     * IllegalStateException} will be thrown if this method is called
     * more than once on the same object.
     *
     * @param processingEnv environment to access facilities the tool framework
     *                      provides to the processor
     * @throws IllegalStateException if this method is called more than once.
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        mFiler = processingEnv.getFiler();                  // Generate class.
        types = processingEnv.getTypeUtils();            // Get type utils.
        elements = processingEnv.getElementUtils();      // Get class meta.

        typeUtils = new TypeUtils(types, elements);
        logger = new Logger(processingEnv.getMessager());   // Package the log utils.

        // Attempt to get user configuration [moduleName]
        Map<String, String> options = processingEnv.getOptions();
        if (MapUtils.isNotEmpty(options)) {
            moduleName = options.get(KEY_MODULE_NAME);
        }

        if (StringUtils.isNotEmpty(moduleName)) {
            moduleName = moduleName.replaceAll("[^0-9a-zA-Z_]+", "");

            logger.info("The user has configuration the module name, it was [" + moduleName + "]");
        } else {
            logger.error("These no module name, at 'build.gradle', like :\n" +
                    "apt {\n" +
                    "    arguments {\n" +
                    "        moduleName project.getName();\n" +
                    "    }\n" +
                    "}\n");
            throw new RuntimeException("ARouter::Compiler >>> No module name, for more information, look at gradle log.");
        }
        createBaseType();
        logger.info(">>> RouteProcessor init. <<<");
    }

    private MethodSpec.Builder loadIntoMethodOfProviderBuilder;
    private TypeMirror type_Activity;
    private TypeMirror type_Service;
    private TypeMirror fragmentTm;
    private TypeMirror fragmentTmV4;
    private TypeMirror type_method;
    private TypeMirror type_methodCallbackAnnotation;
    private TypeMirror type_methodCallback;
    private TypeElement type_IRouteGroup;
    private TypeElement type_IProviderGroup;
    private TypeElement type_IRouteMethod;
    private ClassName routeMetaCn;
    private ClassName routeTypeCn;

    private void createBaseType() {
        // todo 如下逻辑之前写在创建类文件的地方，抽出来感觉更加清爽一点
        iProvider = elements.getTypeElement(Consts.IPROVIDER).asType();
        type_Activity = elements.getTypeElement(ACTIVITY).asType();
        type_Service = elements.getTypeElement(SERVICE).asType();
        fragmentTm = elements.getTypeElement(FRAGMENT).asType();
        fragmentTmV4 = elements.getTypeElement(Consts.FRAGMENT_V4).asType();
        type_method = elements.getTypeElement(METHOD).asType();
        type_methodCallbackAnnotation = elements.getTypeElement(METHOD_CALLBACK_ANNOTATION).asType();
        type_methodCallback = elements.getTypeElement(METHOD_CALLBACK).asType();
        type_IRouteGroup = elements.getTypeElement(IROUTE_GROUP);
        type_IProviderGroup = elements.getTypeElement(IPROVIDER_GROUP);
        type_IRouteMethod = elements.getTypeElement(IROUTE_METHOD);
        routeMetaCn = ClassName.get(RouteMeta.class);
        routeTypeCn = ClassName.get(RouteType.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param annotations
     * @param roundEnv
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (CollectionUtils.isNotEmpty(annotations)) {
            Set<? extends Element> routeElements = roundEnv.getElementsAnnotatedWith(Route.class);
            try {
                logger.info(">>> Found routes, start... <<<");
                this.parseRoutes(routeElements);

            } catch (Exception e) {
                logger.error(e);
            }
            return true;
        }

        return false;
    }

    private void parseRoutes(Set<? extends Element> routeElements) throws Exception {
        if (CollectionUtils.isNotEmpty(routeElements)) {
            // Perpare the type an so on.

            logger.info(">>> Found routes, size is " + routeElements.size() + " <<<");

            rootMap.clear();
            /*
               Build input type, format as :

               ```Map<String, Class<? extends IRouteGroup>>```
             */
            ParameterizedTypeName inputMapTypeOfRoot = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ParameterizedTypeName.get(
                            ClassName.get(Class.class),
                            WildcardTypeName.subtypeOf(ClassName.get(type_IRouteGroup))
                    )
            );

            /*

              ```Map<String, RouteMeta>```
             */
            ParameterizedTypeName inputMapTypeOfGroup = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ClassName.get(RouteMeta.class)
            );

            /*
              Build input param name.
             */
            ParameterSpec rootParamSpec = ParameterSpec.builder(inputMapTypeOfRoot, "routes").build();
            ParameterSpec groupParamSpec = ParameterSpec.builder(inputMapTypeOfGroup, "atlas").build();
            ParameterSpec providerParamSpec = ParameterSpec.builder(inputMapTypeOfGroup, "providers").build();  // Ps. its param type same as groupParamSpec!

            /*
              Build method : 'loadInto'
             */
            MethodSpec.Builder loadIntoMethodOfRootBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(rootParamSpec);

            //  Follow a sequence, find out metas of group first, generate java file, then statistics them as root.
            for (Element element : routeElements) {
                TypeMirror tm = element.asType();
                Route route = element.getAnnotation(Route.class);
                RouteMeta routeMeta = null;

                if (types.isSubtype(tm, type_Activity)) {                 // Activity
                    logger.info(">>> Found activity route: " + tm.toString() + " <<<");

                    // Get all fields annotation by @Autowired
                    Map<String, Integer> paramsType = new HashMap<>();
                    for (Element field : element.getEnclosedElements()) {
                        if (field.getKind().isField() && field.getAnnotation(Autowired.class) != null && !types.isSubtype(field.asType(), iProvider)) {
                            // It must be field, then it has annotation, but it not be provider.
                            Autowired paramConfig = field.getAnnotation(Autowired.class);
                            paramsType.put(StringUtils.isEmpty(paramConfig.name()) ? field.getSimpleName().toString() : paramConfig.name(), typeUtils.typeExchange(field));
                        }
                    }
                    routeMeta = new RouteMeta(route, element, RouteType.ACTIVITY, paramsType);
                    parseMethod(element, routeMeta);
                } else if (types.isSubtype(tm, iProvider)) {         // IProvider
                    logger.info(">>> Found provider route: " + tm.toString() + " <<<");
                    routeMeta = new RouteMeta(route, element, RouteType.PROVIDER, null);
                    parseMethod(element, routeMeta);
                } else if (types.isSubtype(tm, type_Service)) {           // Service
                    logger.info(">>> Found service route: " + tm.toString() + " <<<");
                    routeMeta = new RouteMeta(route, element, RouteType.parse(SERVICE), null);
                } else if (types.isSubtype(tm, fragmentTm) || types.isSubtype(tm, fragmentTmV4)) {
                    logger.info(">>> Found fragment route: " + tm.toString() + " <<<");
                    routeMeta = new RouteMeta(route, element, RouteType.parse(FRAGMENT), null);
                } else {
                    throw new RuntimeException("ARouter::Compiler >>> Found unsupported class type, type = [" + types.toString() + "].");
                }

                categories(routeMeta);
            }

            loadIntoMethodOfProviderBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(providerParamSpec);

            // Start generate java source, structure is divided into upper and lower levels, used for demand initialization.
            for (Map.Entry<String, Set<RouteMeta>> entry : groupMap.entrySet()) {
                String groupName = entry.getKey();

                MethodSpec.Builder loadIntoMethodOfGroupBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(groupParamSpec);

                // Build group method body
                Set<RouteMeta> groupData = entry.getValue();
                for (RouteMeta routeMeta : groupData) {
                    switch (routeMeta.getType()) {
                        case PROVIDER:  // Need cache provider's super class
                            List<? extends TypeMirror> interfaces = ((TypeElement) routeMeta.getRawType()).getInterfaces();
                            for (TypeMirror tm : interfaces) {
                                String className = tm.toString();
                                if (types.isSameType(tm, iProvider) || types.isSubtype(tm, iProvider)) {
                                    createProviderMethod(className, routeMeta);
                                }
                                // todo 之前的代码实在太冗余了,就是下面这堆
//                                if (types.isSameType(tm, iProvider)) {   // Its implements iProvider interface himself.
//                                    // This interface extend the IProvider, so it can be used for mark provider
//                                    loadIntoMethodOfProviderBuilder.addStatement(
//                                            "providers.put($S, $T.build($T." + routeMeta.getType() + ", $T.class, $S, $S, null, " + routeMeta.getPriority() + ", " + routeMeta.getExtra() + "))",
//                                            (routeMeta.getRawType()).toString(),
//                                            routeMetaCn,
//                                            routeTypeCn,
//                                            ClassName.get((TypeElement) routeMeta.getRawType()),
//                                            routeMeta.getPath(),
//                                            routeMeta.getGroup());
//                                } else if (types.isSubtype(tm, iProvider)) {
//                                    // This interface extend the IProvider, so it can be used for mark provider
//                                    loadIntoMethodOfProviderBuilder.addStatement(
//                                            "providers.put($S, $T.build($T." + routeMeta.getType() + ", $T.class, $S, $S, null, " + routeMeta.getPriority() + ", " + routeMeta.getExtra() + "))",
//                                            tm.toString(),    // So stupid, will duplicate only save class name.
//                                            routeMetaCn,
//                                            routeTypeCn,
//                                            ClassName.get((TypeElement) routeMeta.getRawType()),
//                                            routeMeta.getPath(),
//                                            routeMeta.getGroup());
//                                }
                            }
                            break;
                        default:
                            break;
                    }

                    // Make map body for paramsType
                    StringBuilder mapBodyBuilder = new StringBuilder();
                    Map<String, Integer> paramsType = routeMeta.getParamsType();
                    if (MapUtils.isNotEmpty(paramsType)) {
                        for (Map.Entry<String, Integer> types : paramsType.entrySet()) {
                            mapBodyBuilder.append("put(\"").append(types.getKey()).append("\", ").append(types.getValue()).append("); ");
                        }
                    }
                    createGroupMethod(loadIntoMethodOfGroupBuilder, mapBodyBuilder, routeMeta);
                }

                // Generate groups
                String groupFileName = NAME_OF_GROUP + groupName;
                JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                        TypeSpec.classBuilder(groupFileName)
                                .addJavadoc(WARNING_TIPS)
                                .addSuperinterface(ClassName.get(type_IRouteGroup))
                                .addModifiers(PUBLIC)
                                .addMethod(loadIntoMethodOfGroupBuilder.build())
                                .build()
                ).build().writeTo(mFiler);

                logger.info(">>> Generated group: " + groupName + "<<<");
                rootMap.put(groupName, groupFileName);
            }

            if (MapUtils.isNotEmpty(rootMap)) {
                // Generate root meta by group name, it must be generated before root, then I can find out the class of group.
                for (Map.Entry<String, String> entry : rootMap.entrySet()) {
                    loadIntoMethodOfRootBuilder.addStatement("routes.put($S, $T.class)", entry.getKey(), ClassName.get(PACKAGE_OF_GENERATE_FILE, entry.getValue()));
                }
            }

            // Wirte provider into disk
            String providerMapFileName = NAME_OF_PROVIDER + SEPARATOR + moduleName;
            JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                    TypeSpec.classBuilder(providerMapFileName)
                            .addJavadoc(WARNING_TIPS)
                            .addSuperinterface(ClassName.get(type_IProviderGroup))
                            .addModifiers(PUBLIC)
                            .addMethod(loadIntoMethodOfProviderBuilder.build())
                            .build()
            ).build().writeTo(mFiler);

            logger.info(">>> Generated provider map, name is " + providerMapFileName + " <<<");

            // Write root meta into disk.
            String rootFileName = NAME_OF_ROOT + SEPARATOR + moduleName;
            JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                    TypeSpec.classBuilder(rootFileName)
                            .addJavadoc(WARNING_TIPS)
                            .addSuperinterface(ClassName.get(elements.getTypeElement(ITROUTE_ROOT)))
                            .addModifiers(PUBLIC)
                            .addMethod(loadIntoMethodOfRootBuilder.build())
                            .build()
            ).build().writeTo(mFiler);
            createMethodsFile();
            logger.info(">>> Generated root, name is " + rootFileName + " <<<");

        }
    }

    private void createGroupMethod(MethodSpec.Builder loadIntoMethodOfGroupBuilder, StringBuilder mapBodyBuilder, RouteMeta routeMeta) throws Exception {
        String mapBody = mapBodyBuilder.toString();
        String qualifiedName = (routeMeta.getRawType()).toString();
        if (null != providerPaths.get(qualifiedName)) {
            loadIntoMethodOfGroupBuilder.addStatement(
                    "atlas.put($S, $T.build($T." + routeMeta.getType() + ", $T.class, $S, $S, " + (StringUtils.isEmpty(mapBody) ? null : ("new java.util.HashMap<String, Integer>(){{" + mapBodyBuilder.toString() + "}}")) + ", " + routeMeta.getPriority() + ", " + routeMeta.getExtra() + ", $T.class))",
                    routeMeta.getPath(),
                    routeMetaCn,
                    routeTypeCn,
                    ClassName.get((TypeElement) routeMeta.getRawType()),
                    routeMeta.getPath().toLowerCase(),
                    routeMeta.getGroup().toLowerCase(),
                    PathUtils.pathExchange2MethodClass(providerPaths.get(qualifiedName)));
        } else {
            loadIntoMethodOfGroupBuilder.addStatement(
                    "atlas.put($S, $T.build($T." + routeMeta.getType() + ", $T.class, $S, $S, " + (StringUtils.isEmpty(mapBody) ? null : ("new java.util.HashMap<String, Integer>(){{" + mapBodyBuilder.toString() + "}}")) + ", " + routeMeta.getPriority() + ", " + routeMeta.getExtra() + ",null))",
                    routeMeta.getPath(),
                    routeMetaCn,
                    routeTypeCn,
                    ClassName.get((TypeElement) routeMeta.getRawType()),
                    routeMeta.getPath().toLowerCase(),
                    routeMeta.getGroup().toLowerCase());
        }
    }

    private void createProviderMethod(String className, RouteMeta routeMeta) throws Exception {
        String qualifiedName = (routeMeta.getRawType()).toString();
        if (null != providerPaths.get(qualifiedName)) {
            loadIntoMethodOfProviderBuilder
                    .addStatement(
                            "providers.put($S, $T.build($T." + routeMeta.getType() + ", $T.class, $S, $S, null, " + routeMeta.getPriority() + ", " + routeMeta.getExtra() + ", $T.class))",
                            className,
                            routeMetaCn,
                            routeTypeCn,
                            ClassName.get((TypeElement) routeMeta.getRawType()),
                            routeMeta.getPath(),
                            routeMeta.getGroup(),
                            PathUtils.pathExchange2MethodClass(providerPaths.get(qualifiedName))
                    );
        } else {
            loadIntoMethodOfProviderBuilder
                    .addStatement(
                            "providers.put($S, $T.build($T." + routeMeta.getType() + ", $T.class, $S, $S, null, " + routeMeta.getPriority() + ", " + routeMeta.getExtra() + ",null))",
                            className,
                            routeMetaCn,
                            routeTypeCn,
                            ClassName.get((TypeElement) routeMeta.getRawType()),
                            routeMeta.getPath(),
                            routeMeta.getGroup()
                    );
        }
    }

    private void createMethodsFile() throws Exception {
        List<MethodModel> methodModels;
        List<ParamModel> parametersList;
        for (String className : methods.keySet()) {
            // ruan create class like ARouter$$Service$$Ruan$$Method
            TypeSpec.Builder classBuilder = TypeSpec
                    .classBuilder(PathUtils.pathExchange2MethodClassName(providerPaths.get(className)))
                    .addSuperinterface(ClassName.get(type_IRouteMethod))
                    .addModifiers(PUBLIC);
            // ruan create loadMethodInfo
            MethodSpec.Builder methodLoad = MethodSpec
                    .methodBuilder("loadMethodInfo")
                    .addModifiers(PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(ParameterizedTypeName
                            .get(ClassName.get(List.class), ClassName.get(MethodModel.class)), "methods");

            methodModels = methods.get(className);
            for (MethodModel methodModel : methodModels) {
                String paramsMethodName = "loadMethod" + methodModel.getMethodName() + "Params";

                // ruan create loadMethodxxxxParams
                MethodSpec.Builder methodParams = MethodSpec
                        .methodBuilder(paramsMethodName)
                        .addModifiers(PUBLIC)
                        .returns(ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(ParamModel.class)))
                        .addStatement("$T params = new $T()"
                                , ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(ParamModel.class))
                                , ArrayList.class);
                parametersList = methodModel.getParams();

                if (null != parametersList) {
                    for (ParamModel paramType : parametersList) {
                        if (null != paramType.getParamMirror()) {
                            methodParams.addStatement("params.add(new $T($T.class,null))", ParamModel.class, TypeName.get((TypeMirror) paramType.getParamMirror()));
                        } else {
                            methodParams.addStatement("params.add(new $T(com.alibaba.android.arouter.facade.model.MethodCallback.class,null))", ParamModel.class);

                        }
                    }
                    methodParams.addStatement("return params");
                } else {
                    methodParams.addStatement("params = null");
                    methodParams.addStatement("return params");
                }
                methodLoad.addStatement("methods.add(new $T($S,$L()))", MethodModel.class, methodModel.getMethodName(), paramsMethodName);
                // ruan add  loadMethodxxxxParams
                classBuilder.addMethod(methodParams.build());
            }
            // ruan add  loadMethodInfo
            classBuilder.addMethod(methodLoad.build());
            // create class file like ARouter$$Service$$Ruan$$Method.java
            JavaFile.builder(PACKAGE_OF_GENERATE_FILE, classBuilder.build()).build().writeTo(mFiler);
        }

    }

    private void parseMethod(Element element, RouteMeta routeMeta) {

        String className = ((TypeElement) element).getQualifiedName().toString();

        List<? extends Element> providerChildElements = element.getEnclosedElements();
        List<? extends VariableElement> parameters;

        String methodName;
        for (Element providerChildElement : providerChildElements) {// check each filed of provider
            if (TypeUtils.isMethodAndAnnotatedByTargetType(providerChildElement, type_method)) { //filed is a method and annotated by @Method or @AsynMethod
                providerPaths.put(className, routeMeta.getPath());
                parameters = ((ExecutableElement) providerChildElement).getParameters();
                methodName = providerChildElement.getSimpleName().toString();
                List<MethodModel> methodModels = methods.get(className);
                if (null == methodModels) {
                    methodModels = new ArrayList<>(5);
                    methods.put(className, methodModels);
                }
                List<ParamModel> parametersList = null;
                for (VariableElement parameter : parameters) {
                    if (null == parametersList) {
                        parametersList = new ArrayList<>();
                    }
                    if (TypeUtils.isParameterAndAnnotatedByTargetType(parameter, type_methodCallbackAnnotation)) {
                        parametersList.add(new ParamModel(null, null));
                    } else {
                        parametersList.add(new ParamModel(null, parameter.asType()));
                    }
                }
                methodModels.add(new MethodModel(methodName, parametersList));
            }
        }

    }

    /**
     * Sort metas in group.
     *
     * @param routeMete metas.
     */
    private void categories(RouteMeta routeMete) {
        if (routeVerify(routeMete)) {
            logger.info(">>> Start categories, group = " + routeMete.getGroup() + ", path = " + routeMete.getPath() + " <<<");
            Set<RouteMeta> routeMetas = groupMap.get(routeMete.getGroup());
            if (CollectionUtils.isEmpty(routeMetas)) {
                Set<RouteMeta> routeMetaSet = new TreeSet<>(new Comparator<RouteMeta>() {
                    @Override
                    public int compare(RouteMeta r1, RouteMeta r2) {
                        try {
                            return r1.getPath().compareTo(r2.getPath());
                        } catch (NullPointerException npe) {
                            logger.error(npe.getMessage());
                            return 0;
                        }
                    }
                });
                routeMetaSet.add(routeMete);
                groupMap.put(routeMete.getGroup(), routeMetaSet);
            } else {
                routeMetas.add(routeMete);
            }
        } else {
            logger.warning(">>> Route meta verify error, group is " + routeMete.getGroup() + " <<<");
        }
    }

    /**
     * Verify the route meta
     *
     * @param meta raw meta
     */
    private boolean routeVerify(RouteMeta meta) {
        String path = meta.getPath();

        if (StringUtils.isEmpty(path) || !path.startsWith("/")) {   // The path must be start with '/' and not empty!
            return false;
        }

        if (StringUtils.isEmpty(meta.getGroup())) { // Use default group(the first word in path)
            try {
                String defaultGroup = path.substring(1, path.indexOf("/", 1));
                if (StringUtils.isEmpty(defaultGroup)) {
                    return false;
                }

                meta.setGroup(defaultGroup);
                return true;
            } catch (Exception e) {
                logger.error("Failed to extract default group! " + e.getMessage());
                return false;
            }
        }

        return true;
    }
}
