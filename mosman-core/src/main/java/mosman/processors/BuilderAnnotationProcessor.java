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
@SupportedAnnotationTypes("mosman.annotations.Builder")
@AutoService(Processor.class)
public class BuilderAnnotationProcessor extends AbstractMosmanAnnotationProcessor {

    @Override
    protected final void processType(final Element element, final Filer filer) throws IOException {
        String className = format("%sBuilder", element.getSimpleName());

        TypeSpec.Builder dataClass = classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(element);
        String packageName = packageElement.toString();

        MethodSpec.Builder constructor = constructorBuilder().addModifiers(Modifier.PROTECTED);
        dataClass.addMethod(constructor.build());

        element.getEnclosedElements()
                .stream()
                .filter(e -> e instanceof ExecutableElement)
                .map(e -> (ExecutableElement) e)
                .filter(e -> !e.isDefault() || !e.getModifiers().contains(Modifier.STATIC))
                .forEach(item -> {
                    TypeName typeName = TypeName.get(item.getReturnType());
                    String name = item.getSimpleName().toString();

                    dataClass.addField(typeName, name, Modifier.PRIVATE);

                    dataClass.addMethod(
                            methodBuilder(format("_%s", item.getSimpleName().toString()))
                                    .returns(ClassName.get(packageName, className))
                                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                    .addParameter(typeName, name)
                                    .addStatement(format("this.%s = %s", name, name))
                                    .addStatement("return this")
                                    .build()
                    );

                    dataClass.addMethod(
                            methodBuilder(item.getSimpleName().toString())
                                    .returns(typeName)
                                    .addModifiers(Modifier.PROTECTED, Modifier.FINAL)
                                    .addStatement(format("return this.%s", name))
                                    .build()
                    );
                });

        dataClass.addMethod(
                methodBuilder("build")
                        .returns(ClassName.get(packageName, element.getSimpleName().toString()))
                        .addStatement(format("return new %sImpl(this)", element.getSimpleName()))
                        .build()
        );

        JavaFile.builder(packageName, dataClass.build())
                .build()
                .writeTo(filer);
    }

}
