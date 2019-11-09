package mosman.processors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Set;

public abstract class AbstractMosmanAnnotationProcessor extends AbstractProcessor {

    @Override
    public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final Messager messager = processingEnv.getMessager();

        // fail when annotation is not applied to interface
        boolean notInterfaces = annotations.stream()
                .flatMap(a -> roundEnv.getElementsAnnotatedWith(a).stream())
                .anyMatch(e -> !e.getKind().isInterface());

        if (notInterfaces) {
            messager.printMessage(Diagnostic.Kind.ERROR, "@mosman.* should only be applied to interfaces");
            return true;
        }

        final Filer filer = processingEnv.getFiler();

        annotations.stream()
                .flatMap(a -> roundEnv.getElementsAnnotatedWith(a).stream())
                .forEach(element -> {
                    try {
                        messager.printMessage(
                                Diagnostic.Kind.MANDATORY_WARNING,
                                "processing @mosman.* at " + element
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

    protected abstract void processType(Element element, Filer filer) throws IOException;
}
