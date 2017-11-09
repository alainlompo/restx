package restx.common;

import com.google.common.base.Function;

import java.lang.annotation.Annotation;

public class MoreAnnotations {
	
	private MoreAnnotations() {
	}
	
    public static final Function<Annotation, Class<? extends Annotation>> EXTRACT_ANNOTATION_TYPE = input -> input.annotationType();

}
