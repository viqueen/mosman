package mosman.processors;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Set;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.lang.String.format;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("mosman.annotations.Data")
@AutoService(Processor.class)
public class DataAnnotationProcessor extends AbstractProcessor {

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final Messager messager = processingEnv.getMessager();
        final Filer filer = processingEnv.getFiler();

        // fail when annotation is not applied to interface
        boolean notInterfaces = annotations.stream()
                .flatMap(a -> roundEnv.getElementsAnnotatedWith(a).stream())
                .anyMatch(e -> !e.getKind().isInterface());

        if (notInterfaces) {
            messager.printMessage(Diagnostic.Kind.ERROR, "@Data should only be applied to interfaces");
            return true;
        }

        annotations.stream()
                .flatMap(a -> roundEnv.getElementsAnnotatedWith(a).stream())
                .forEach(element -> {
                    try {
                        messager.printMessage(
                                Diagnostic.Kind.MANDATORY_WARNING,
                                "processing @Data at " + element
                        );
                        processType(element, filer);
                    } catch (IOException e) {
                        messager.printMessage(
                                Diagnostic.Kind.ERROR,
                                e.getMessage()
                        );
                    }
                });
        return true;
    }

    private void processType(Element element, Filer filer) throws IOException {
        String className = format("%sData", element.getSimpleName());

        TypeSpec.Builder dataClass = classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(TypeName.get(element.asType()));

        MethodSpec.Builder constructor = constructorBuilder();

        element.getEnclosedElements()
                .stream()
                .filter(e -> e instanceof ExecutableElement)
                .map(e -> (ExecutableElement) e)
                .forEach(item -> {
                    TypeName typeName = TypeName.get(item.getReturnType());
                    String name = item.getSimpleName().toString();

                    dataClass.addField(typeName, name, Modifier.PRIVATE, Modifier.FINAL);

                    constructor.addParameter(typeName, name)
                            .addStatement(format("this.%s = %s", name, name));

                    dataClass.addMethod(
                            methodBuilder(item.getSimpleName().toString())
                                    .addAnnotation(Override.class)
                                    .returns(typeName)
                                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                    .addStatement(format("return this.%s", name))
                                    .build()
                    );
                });

        dataClass.addMethod(constructor.build());

        PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(element);

        JavaFile.builder(packageElement.toString(), dataClass.build())
                .build()
                .writeTo(filer);
    }

}
