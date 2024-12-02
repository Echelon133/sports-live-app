package ml.echelon133.matchservice;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.SpringWebConstraintValidatorFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

import jakarta.validation.ConstraintValidator;
import java.util.Map;

/**
 * Factory of {@link LocalValidatorFactoryBean} instances which are configured to produce our custom
 * {@link ConstraintValidator}s while using standalone {@link MockMvc}.
 *
 * <p>
 *     Without configuring standalone {@link MockMvc}'s `setValidator`, any custom {@link ConstraintValidator} which has
 *     dependencies will simply be instantiated without these dependencies. This causes
 *     unexpected {@link NullPointerException}s inside of `isValid` of validators during tests.
 * </p>
 * <p>
 *     This factory enables creation of {@link LocalValidatorFactoryBean} instances which additionally can provide
 *     instances of custom validators configured by us.
 * </p>
 * <p>
 *     The code below shows how to create a {@link LocalValidatorFactoryBean} that returns a configured instance
 *     of `CountryExists.Validator` every time that validator is requested.
 * </p>
 * <pre>
 *     var customValidators = Map.of(CountryExists.Validator.class, new CountryExists.Validator(someDependency));
 *     var validatorFactory = TestValidatorFactory.getInstance(customValidators);
 * </pre>
 * <p>
 *     A standalone {@link MockMvc} configuration can use such a factory by setting it with the `setValidator` method.
 * </p>
 */
// SAFETY: this class needs to hold a map
//      *   where the keys are `Class<? extends ConstraintValidator<? extends Annotation, String>>`
//      *   where the values are `? extends ConstraintValidator<? extends Annotation, String>`
//  This type is too verbose and difficult to read, that's why the ConstraintValidator is instead used as a raw type
//  with suppressed warnings. This class is exclusively used for setting up test environments for controllers,
//  which means that any `ClassCastException` problems caused by this class can only impact the test environment,
//  therefore there is no large risk caused by these warnings being suppressed.
@SuppressWarnings("unchecked")
public class TestValidatorFactory {

    /**
     * Creates an instance of {@link LocalValidatorFactoryBean} which will be able to provide
     * any custom validators during the execution of controller's tests while using standalone {@link MockMvc} configuration.
     * @param customValidators information about the instances of validators which have to be provided when a certain
     *                         validator is requested during the execution of tests
     * @return a configured factory of validators
     */
    public static LocalValidatorFactoryBean getInstance(Map<Class<? extends ConstraintValidator>, ? extends ConstraintValidator> customValidators) {
        var localValidatorFactoryBean = new LocalValidatorFactoryBean();
        localValidatorFactoryBean.setConstraintValidatorFactory(new TestConstraintValidatorFactory(customValidators));
        localValidatorFactoryBean.afterPropertiesSet();
        return localValidatorFactoryBean;
    }

    static private class TestConstraintValidatorFactory extends SpringWebConstraintValidatorFactory {
        private final WebApplicationContext ctx;
        private final Map<Class<? extends ConstraintValidator>, ? extends ConstraintValidator> customValidators;

        public TestConstraintValidatorFactory(Map<Class<? extends ConstraintValidator>, ? extends ConstraintValidator> customValidators) {
            var context = new GenericWebApplicationContext();
            context.refresh();
            this.ctx = context;
            this.customValidators = customValidators;
        }

        @Override
        public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
            if (this.customValidators.containsKey(key)) {
                return (T) this.customValidators.get(key);
            } else {
                return super.getInstance(key);
            }
        }

        @Override
        protected WebApplicationContext getWebApplicationContext() {
            return ctx;
        }
    }
}
