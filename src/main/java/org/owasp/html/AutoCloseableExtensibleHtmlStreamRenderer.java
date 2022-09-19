// Copyright (c) 2011, Mike Samuel
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//
// Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// Neither the name of the OWASP nor the names of its contributors may
// be used to endorse or promote products derived from this software
// without specific prior written permission.
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
// BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
// ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package org.owasp.html;

import javax.annotation.WillCloseWhenClosed;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Access main file here:
 * https://github.com/Zimbra/java-html-sanitizer-release-20190610.1/blob/develop/src/main/java/org/owasp/html/AutoCloseableHtmlStreamRenderer.java
 */
final class AutoCloseableExtensibleHtmlStreamRenderer extends ExtensibleHtmlStreamRenderer
        // This is available on JDK6 and makes this class extend AutoCloseable.
        implements Closeable {
    private final Object closeable;

    private static final Class<?> CLASS_AUTO_CLOSEABLE;

    static {
        Class<?> classAutoCloseable = null;
        for (Class<?> superInterface : Closeable.class.getInterfaces()) {
            if ("java.lang.AutoCloseable".equals(superInterface.getName())) {
                classAutoCloseable = superInterface;
                break;
            }
        }
        CLASS_AUTO_CLOSEABLE = classAutoCloseable;
    }

    private static final Method METHOD_CLOSE;

    static {
        Method methodClose = null;
        if (CLASS_AUTO_CLOSEABLE != null) {
            try {
                methodClose = CLASS_AUTO_CLOSEABLE.getMethod("close");
            } catch (NoSuchMethodException ex) {
                throw (NoSuchMethodError) new NoSuchMethodError().initCause(ex);
            }
        }
        METHOD_CLOSE = methodClose;
    }

    static boolean isAutoCloseable(Object o) {
        return o instanceof Closeable || CLASS_AUTO_CLOSEABLE != null && CLASS_AUTO_CLOSEABLE.isInstance(o);
    }

    // ZBUG-2547 change to accept zimbra_strict_unclosed_comment_tag and
    // zimbra_strict_unclosed_comment_tag local config value
    static AutoCloseableExtensibleHtmlStreamRenderer createAutoCloseableHtmlStreamRenderer(
            @WillCloseWhenClosed Appendable output, Handler<? super IOException> errorHandler,
            Handler<? super String> badHtmlHandler, boolean strictUnclosedCDATACheck,
            Set<String> skipTagsUnclosedCdata) {
        return new AutoCloseableExtensibleHtmlStreamRenderer(output, errorHandler, badHtmlHandler,
                strictUnclosedCDATACheck, skipTagsUnclosedCdata);
    }

    // ZBUG-2547 change to accept zimbra_strict_unclosed_comment_tag and
    // zimbra_strict_unclosed_comment_tag local config value
    private AutoCloseableExtensibleHtmlStreamRenderer(@WillCloseWhenClosed Appendable output,
            Handler<? super IOException> errorHandler, Handler<? super String> badHtmlHandler,
            boolean strictUnclosedCDATACheck, Set<String> skipTagsUnclosedCdata) {
        super(output, errorHandler, badHtmlHandler, strictUnclosedCDATACheck, skipTagsUnclosedCdata);
        this.closeable = output;
    }

    private static final Object[] ZERO_OBJECTS = new Object[0];

    public void close() throws IOException {
        if (isDocumentOpen()) {
            closeDocument();
        }
        closeIfAnyCloseable(closeable);
    }

    static void closeIfAnyCloseable(Object closeable) throws IOException {
        if (closeable instanceof Closeable) {
            ((Closeable) closeable).close();
        } else if (METHOD_CLOSE != null) {
            try {
                METHOD_CLOSE.invoke(closeable, ZERO_OBJECTS);
            } catch (IllegalAccessException ex) {
                AssertionError ae = new AssertionError("close not public");
                ae.initCause(ex);
                throw ae;
            } catch (InvocationTargetException ex) {
                Throwable tgt = ex.getTargetException();
                if (tgt instanceof IOException) {
                    throw (IOException) tgt;
                } else if (tgt instanceof RuntimeException) {
                    throw (RuntimeException) tgt;
                } else {
                    throw new AssertionError(null, tgt);
                }
            }
        }
    }
}
