package ru.veryevilzed.tools;

import com.squareup.javapoet.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    Filer filer;
    Messager messager;
    private ElementTypePair smartClassServiceType;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.smartClassServiceType = getType(SCS_TYPE);
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


        List<ExecutableServiceElement> executableServiceElements = new ArrayList<>();

        for (TypeElement typeElement : ElementFilter.typesIn(entityAnnotated)) {
            messager.printMessage(Diagnostic.Kind.NOTE, "Build Service for " + typeElement.getSimpleName());
            SmartClassService serviceAnnotation = typeElement.getAnnotation(SmartClassService.class);
            // Находим методы
            for (ExecutableElement executableElement : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
                messager.printMessage(Diagnostic.Kind.NOTE, "  found method " + executableElement.getSimpleName());
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
        TypeSpec.Builder serviceBuilder = TypeSpec.classBuilder("ClassServiceImpl")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassService.class)
                .addAnnotation(Service.class);

        if (elements.size() == 0)
            return;


        Map<String, MethodSpec.Builder> methods = new HashMap<>();
        Set<Name> services = new HashSet<>();

        MethodSpec.Builder commonMethod = MethodSpec.methodBuilder("execute")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Object.class, "incoming")
                .addParameter(Object.class, "context")
                .returns(Object.class);


        for (ExecutableServiceElement element : elements) {
            if (!services.contains(element.typeElement.getSimpleName())) {
                services.add(element.typeElement.getSimpleName());
                serviceBuilder.addField(
                        FieldSpec.builder(ClassName.get(element.typeElement), element.fieldServiceName())
                                .addAnnotation(Autowired.class)
                                .build()
                );
            }

            ElementTypePair valueTypePair = getType(element.serviceAnnotation.value());
            ElementTypePair contextTypePair = getType(element.serviceAnnotation.context());

            if (!methods.containsKey(element.serviceAnnotation.value())){
                MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("namedExecute")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(ClassName.get(contextTypePair.element))
                        .addParameter(ClassName.get(valueTypePair.element), "incoming")
                        .addParameter(ClassName.get(contextTypePair.element), "context");

                methods.put(element.serviceAnnotation.value(), methodSpecBuilder);

                commonMethod.addCode("if (incoming instanceof $T && context instanceof $T) return namedExecute(($T)incoming, ($T)context);\n", valueTypePair.element, contextTypePair.element, valueTypePair.element, contextTypePair.element);

            }

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

        serviceBuilder.addMethod(commonMethod.addCode("return context;\n").build());

        JavaFile javaFile = JavaFile.builder("ru.veryevilzed.tools", serviceBuilder.build())
                .build();
        javaFile.writeTo(filer);
    }
}
