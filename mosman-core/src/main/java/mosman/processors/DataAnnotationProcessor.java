package mosman.processors;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import java.io.IOException;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.lang.String.format;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("mosman.annotations.Data")
@AutoService(Processor.class)
public class DataAnnotationProcessor extends AbstractMosmanAnnotationProcessor {

    @Override
    protected final void processType(final Element element, Filer filer) throws IOException {
        String className = format("%sImpl", element.getSimpleName());
        PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(element);
        String packageName = packageElement.toString();

        ClassName builderClazz = ClassName.get(packageName, format("%sBuilder", element.getSimpleName()));

        TypeSpec.Builder dataClass = classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(TypeName.get(element.asType()));

        MethodSpec.Builder constructor = constructorBuilder().addModifiers(Modifier.PROTECTED);
        constructor.addParameter(builderClazz, "builder");

        element.getEnclosedElements()
                .stream()
                .filter(e -> e instanceof ExecutableElement)
                .map(e -> (ExecutableElement) e)
                .filter(e -> !e.isDefault() || !e.getModifiers().contains(Modifier.STATIC))
                .forEach(item -> {
                    TypeName typeName = TypeName.get(item.getReturnType());
                    String name = item.getSimpleName().toString();

                    dataClass.addField(typeName, name, Modifier.PRIVATE, Modifier.FINAL);

                    constructor.addStatement(format("this.%s = builder.%s()", name, name));

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
        dataClass.addMethod(
                methodBuilder("builder")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(builderClazz)
                        .addStatement(format("return new %sBuilder()", element.getSimpleName()))
                        .build()
        );

        JavaFile.builder(packageName, dataClass.build())
                .build()
                .writeTo(filer);
    }

}
