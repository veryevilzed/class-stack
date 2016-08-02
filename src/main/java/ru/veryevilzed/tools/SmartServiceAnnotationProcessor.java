package ru.veryevilzed.tools;

import com.squareup.javapoet.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.remoting.rmi.CodebaseAwareObjectInputStream;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;

/**
 * Created by zed on 02.08.16.
 */
@SupportedAnnotationTypes({SmartServiceAnnotationProcessor.SCS_TYPE})
public class SmartServiceAnnotationProcessor extends AbstractProcessor {

    final static String SCS_TYPE = "ru.veryevilzed.tools.SmartClassService";
    final static String SCM_TYPE = "ru.veryevilzed.tools.SmartClassMethod";

    Filer filer;
    Messager messager;
    private ElementTypePair smartClassServiceType;
    private ElementTypePair smartClassMethodType;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.smartClassServiceType = getType(SCS_TYPE);
        this.smartClassMethodType = getType(SCM_TYPE);
    }

    private Types typeUtils() {
        return processingEnv.getTypeUtils();
    }

    private ElementTypePair getType(String className) {
        TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(className);
        DeclaredType declaredType = typeUtils().getDeclaredType(typeElement);
        return new ElementTypePair(typeElement, declaredType);
    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        checkSmartClassServiceAnnotatedElement(roundEnv);
        return false;
    }


//    private List<String> createPath(String target, TypeElement element) {
//        List<String> path = new ArrayList<>();
//        path.addAll(Arrays.asList(target.split("\\."));
//        return createPath(path, element);
//    }
//
//    private List<String> createPath(List<String> path, TypeElement element) {
//        for(ExecutableElement e : ElementFilter.methodsIn(element.getEnclosedElements())){
//            if (e.getSimpleName().toString().startsWith("get"+path.get(0))){
//                return
//            }
//        }
//    }

    private CodeBlock createIfPath(String path, String prefix) {


        String[] args = path.split("\\.");
        CodeBlock.Builder res = null;

        for(int i=0;i<args.length;i++){
            List<String> _args = new ArrayList<>();
            for(int j=0;j<i+1;j++)
                _args.add(args[j]);



            if (res == null) {
                res = CodeBlock.builder();
                res.add("if ($L.$L != null", prefix, String.join(".", _args));
            }else{
                res.add(" && $L.$L != null", prefix, String.join(".", _args));
            }
        }
        res.add(")");
        return res.build();
    }

    private void checkSmartClassServiceAnnotatedElement(RoundEnvironment roundEnv){
        Set<? extends Element> entityAnnotated =
                roundEnv.getElementsAnnotatedWith(smartClassServiceType.element);
        // technically, we don't need to filter here, but it gives us a free cast

        List<ExecutableServiceElement> executableServiceElements = new ArrayList<>();

        for (TypeElement typeElement : ElementFilter.typesIn(entityAnnotated)) {
            System.out.println("Element: " + typeElement.getSimpleName());

            SmartClassService serviceAnnotation = typeElement.getAnnotation(SmartClassService.class);
            System.out.println("Annotation is: " + serviceAnnotation.value());
            // Находим методы
            for (ExecutableElement executableElement : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
                SmartClassMethod methodAnnotation = executableElement.getAnnotation(SmartClassMethod.class);
                if (methodAnnotation == null)
                    continue;

                ExecutableServiceElement executableServiceElement = new ExecutableServiceElement();
                executableServiceElement.typeElement = typeElement;
                executableServiceElement.executableElement = executableElement;
                executableServiceElement.methodAnnotation = methodAnnotation;
                executableServiceElement.serviceAnnotation = serviceAnnotation;
                executableServiceElements.add(executableServiceElement);
            }
        }
        try {
            buildService(executableServiceElements);
        }catch (IOException ex) {
            messager.printMessage(Diagnostic.Kind.ERROR, ex.getMessage());
        }

    }

    private void buildService(List<ExecutableServiceElement> elements) throws IOException {
        System.out.println("Build Services: " + elements.size());
        TypeSpec.Builder serviceBuilder = TypeSpec.classBuilder("ClassServiceImpl")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Service.class);
        if (elements.size() == 0)
            return;


        Map<String, MethodSpec.Builder> methods = new HashMap<>();
        Set<Name> services = new HashSet<>();

        for (ExecutableServiceElement element : elements) {
            if (!services.contains(element.typeElement.getSimpleName())) {
                System.out.println("Add autowired for: " + element.fieldServiceName() + " " + ClassName.get(element.typeElement));
                services.add(element.typeElement.getSimpleName());
                serviceBuilder.addField(
                        FieldSpec.builder(ClassName.get(element.typeElement), element.fieldServiceName())
                                .addAnnotation(Autowired.class)
                                .build()
                );
            }
            System.out.println("Ready for create methods:" + element.serviceAnnotation.value());

            ElementTypePair valueTypePair = getType(element.serviceAnnotation.value());
            ElementTypePair contextTypePair = getType(element.serviceAnnotation.context());



            if (!methods.containsKey(element.serviceAnnotation.value())){

                System.out.println("Add method for: " + element.serviceAnnotation);

                MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("execute")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(ClassName.get(contextTypePair.element))
                        .addParameter(ClassName.get(valueTypePair.element), "incoming")
                        .addParameter(ClassName.get(contextTypePair.element), "context");

                methods.put(element.serviceAnnotation.value(), methodSpecBuilder);
            }

            System.out.println("GetMethodBuilder");

            MethodSpec.Builder methodSpecBuilder = methods.get(element.serviceAnnotation.value());


            methodSpecBuilder.addCode(createIfPath(element.methodAnnotation.value(), "incoming"));

            methodSpecBuilder.addCode(" $L.$L(incoming.$L, context);\n",
                    element.fieldServiceName(),
                    element.executableElement.getSimpleName(),
                    element.methodAnnotation.value()
                    );
        }

        for (MethodSpec.Builder method : methods.values())
            serviceBuilder.addMethod(method.addCode("return context;\n").build());


        JavaFile javaFile = JavaFile.builder("ru.veryevilzed.tools", serviceBuilder.build())
                .build();
        javaFile.writeTo(filer);
    }
}
