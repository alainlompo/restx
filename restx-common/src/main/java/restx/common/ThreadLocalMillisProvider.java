package restx.common;

import static com.google.common.base.Preconditions.checkNotNull;

import org.joda.time.DateTimeUtils;

/**
 * A MillisProvider which delegates to other providers with a per thread setting.
 *
 * It is generally used as a replacement for JodaTime millis provider which are classloader wide, and thus
 * not compatible with parallel tests.
 *
 * Note that calling any of the set method will install a ThreadLocalMillisProvider as DateTimeUtils millis provider.
 */
public class ThreadLocalMillisProvider implements DateTimeUtils.MillisProvider {
    private static final DateTimeUtils.MillisProvider systemMillisProvider = System::currentTimeMillis;
    		
    private static final ThreadLocal<DateTimeUtils.MillisProvider> local
            = new ThreadLocal<DateTimeUtils.MillisProvider>() {
        @Override
        protected DateTimeUtils.MillisProvider initialValue() {
            return systemMillisProvider;
        }
    };

    private static final DateTimeUtils.MillisProvider INSTANCE = new ThreadLocalMillisProvider();

    public static final void setCurrentMillisSystem() {
        install();
        local.set(systemMillisProvider);
    }

    public static final void setCurrentMillisFixed(long fixedMillis) {
        install();
        local.set(new FixedMillisProvider(fixedMillis));
    }

    public static final void setCurrentMillisOffset(long offsetMillis) {
        install();
        local.set(new OffsetMillisProvider(offsetMillis));
    }

    public static final void setCurrentMillisProvider(DateTimeUtils.MillisProvider millisProvider) {
        install();
        local.set(checkNotNull(millisProvider));
    }

    public static void clear() {
        local.remove();
    }

    public static DateTimeUtils.MillisProvider current() {
        return local.get();
    }

    private static void install() {
        DateTimeUtils.setCurrentMillisProvider(INSTANCE);
    }

    private ThreadLocalMillisProvider() {

    }

    @Override
    public long getMillis() {
        return local.get().getMillis();
    }

    /**
     * Fixed millisecond provider.
     */
    public static class FixedMillisProvider implements DateTimeUtils.MillisProvider {
        /** The fixed millis value. */
        private final long iMillis;

        /**
         * Constructor.
         * @param fixedMillis  the millis offset
         */
        public FixedMillisProvider(long fixedMillis) {
            iMillis = fixedMillis;
        }

        /**
         * Gets the current time.
         * @return the current time in millis
         */
        public long getMillis() {
            return iMillis;
        }
    }

    /**
     * Offset from system millis provider.
     */
    public static class OffsetMillisProvider implements DateTimeUtils.MillisProvider {
        /** The millis offset. */
        private final long iMillis;

        /**
         * Constructor.
         * @param offsetMillis  the millis offset
         */
        public OffsetMillisProvider(long offsetMillis) {
            iMillis = offsetMillis;
        }

        /**
         * Gets the current time.
         * @return the current time in millis
         */
        public long getMillis() {
            return System.currentTimeMillis() + iMillis;
        }
    }
}
