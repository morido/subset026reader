package helper.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for highly domain specific parts (i.e. those only applicable for the subset026) of this program
 * 
 * @author Moritz Dorka <moritz.dorka@mailbox.tu-dresden.de> 
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface DomainSpecific {
    // good documentation: http://www.oracle.com/technetwork/articles/hunter-meta-2-098036.html
}
