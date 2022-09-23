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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public class ExtensibleHtmlStreamRendererTest extends TestCase {

    private final List<String> errors = Lists.newArrayList();
    private final StringBuilder rendered = new StringBuilder();

    // when zimbra_strict_unclosed_comment_tag = false , then improper div is
    // skipped
    private final ExtensibleHtmlStreamRenderer rendererStrictUnclosedCommentTagFalse = ExtensibleHtmlStreamRenderer
            .create(rendered, new Handler<Throwable>() {
                public void handle(Throwable th) {
                    List<String> errors = ExtensibleHtmlStreamRendererTest.this.errors;
                    errors.add(th.toString());
                }

            }, new Handler<String>() {
                public void handle(String errorMessage) {
                    @SuppressWarnings({ "hiding", "synthetic-access" })
                    List<String> errors = ExtensibleHtmlStreamRendererTest.this.errors;
                    errors.add(errorMessage);
                }
            }, false, new HashSet<>(Arrays.asList("script")));

    // when zimbra_strict_unclosed_comment_tag = true , then as per logic error is
    // thrown
    private final ExtensibleHtmlStreamRenderer rendererStrictUnclosedCommentTagTrue = ExtensibleHtmlStreamRenderer
            .create(rendered, new Handler<Throwable>() {
                public void handle(Throwable th) {
                    List<String> errors = ExtensibleHtmlStreamRendererTest.this.errors;
                    errors.add(th.toString());
                }

            }, new Handler<String>() {
                public void handle(String errorMessage) {
                    @SuppressWarnings({ "hiding", "synthetic-access" })
                    List<String> errors = ExtensibleHtmlStreamRendererTest.this.errors;
                    errors.add(errorMessage);
                }
            }, true, new HashSet<>(Arrays.asList("script")));

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        errors.clear();
        rendered.setLength(0);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        assertTrue(errors.toString(), errors.isEmpty()); // Catch any tests that don't check errors.
    }

    public final void testIllegalElementName() throws Exception {
        rendererStrictUnclosedCommentTagFalse.openDocument();
        rendererStrictUnclosedCommentTagFalse.openTag(":svg", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.openTag("svg:", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.openTag("-1", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.openTag("svg::svg", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.openTag("a@b", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.closeDocument();

        String output = rendered.toString();
        assertFalse(output, output.contains("<"));

        assertEquals(
                Joiner.on('\n').join("Invalid element name : :svg", "Invalid element name : svg:",
                        "Invalid element name : -1", "Invalid element name : svg::svg", "Invalid element name : a@b"),
                Joiner.on('\n').join(errors));
        errors.clear();
    }

    public final void testIllegalAttributeName() throws Exception {
        rendererStrictUnclosedCommentTagFalse.openDocument();
        rendererStrictUnclosedCommentTagFalse.openTag("div", ImmutableList.of(":svg", "x"));
        rendererStrictUnclosedCommentTagFalse.openTag("div", ImmutableList.of("svg:", "x"));
        rendererStrictUnclosedCommentTagFalse.openTag("div", ImmutableList.of("-1", "x"));
        rendererStrictUnclosedCommentTagFalse.openTag("div", ImmutableList.of("svg::svg", "x"));
        rendererStrictUnclosedCommentTagFalse.openTag("div", ImmutableList.of("a@b", "x"));
        rendererStrictUnclosedCommentTagFalse.closeDocument();

        String output = rendered.toString();
        assertFalse(output, output.contains("="));

        assertEquals(Joiner.on('\n').join("Invalid attr name : :svg", "Invalid attr name : svg:",
                "Invalid attr name : -1", "Invalid attr name : svg::svg", "Invalid attr name : a@b"),
                Joiner.on('\n').join(errors));
        errors.clear();
    }

    public final void testCdataContainsEndTag1() throws Exception {
        rendererStrictUnclosedCommentTagTrue.openDocument();
        rendererStrictUnclosedCommentTagTrue.openTag("script", ImmutableList.of("type", "text/javascript"));
        rendererStrictUnclosedCommentTagTrue.text("document.write('<SCRIPT>alert(42)</SCRIPT>')");
        rendererStrictUnclosedCommentTagTrue.closeTag("script");
        rendererStrictUnclosedCommentTagTrue.closeDocument();

        assertEquals("<script type=\"text/javascript\"></script>", rendered.toString());
        assertEquals("Invalid CDATA text content : </SCRIPT>'", Joiner.on('\n').join(errors));
        errors.clear();
    }

    public final void testCdataContainsEndTag2() throws Exception {
        rendererStrictUnclosedCommentTagTrue.openDocument();
        rendererStrictUnclosedCommentTagTrue.openTag("style", ImmutableList.of("type", "text/css"));
        rendererStrictUnclosedCommentTagTrue.text("/* </St");
        // Split into two text chunks, and insert NULs.
        rendererStrictUnclosedCommentTagTrue.text("\0yle> */");
        rendererStrictUnclosedCommentTagTrue.closeTag("style");
        rendererStrictUnclosedCommentTagTrue.closeDocument();

        assertEquals("<style type=\"text/css\"></style>", rendered.toString());
        assertEquals("Invalid CDATA text content : </Style> *", Joiner.on('\n').join(errors));
        errors.clear();
    }

    public final void testRcdataContainsEndTag() throws Exception {
        rendererStrictUnclosedCommentTagFalse.openDocument();
        rendererStrictUnclosedCommentTagFalse.openTag("textarea", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.text("<textarea></textarea>");
        rendererStrictUnclosedCommentTagFalse.closeTag("textarea");
        rendererStrictUnclosedCommentTagFalse.closeDocument();

        assertEquals("<textarea>&lt;textarea&gt;&lt;/textarea&gt;</textarea>", rendered.toString());
    }

    public final void testHtml51SemanticsScriptingExample5Part1() throws Exception {
        String js = "  var example = 'Consider this string: <!-- <script>';\n" + "  console.log(example);\n";

        rendererStrictUnclosedCommentTagTrue.openDocument();
        rendererStrictUnclosedCommentTagTrue.openTag("script", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagTrue.text(js);
        rendererStrictUnclosedCommentTagTrue.closeTag("script");
        rendererStrictUnclosedCommentTagTrue.closeDocument();

        assertEquals("Invalid CDATA text content : <script>';", Joiner.on('\n').join(errors));
        errors.clear();
    }

    public final void testHtml51SemanticsScriptingExample5Part2() throws Exception {
        String js = "if (x<!--y) { ... }\n";

        rendererStrictUnclosedCommentTagTrue.openDocument();
        rendererStrictUnclosedCommentTagTrue.openTag("script", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagTrue.text(js);
        rendererStrictUnclosedCommentTagTrue.closeTag("script");
        rendererStrictUnclosedCommentTagTrue.closeDocument();

        assertEquals("<script></script>", rendered.toString());
        assertEquals("Invalid CDATA text content : <!--y) { .", Joiner.on('\n').join(errors));
        errors.clear();
    }

    public final void testMoreUnbalancedHtmlCommentsInScripts() throws Exception {
        String js = "if (x-->y) { ... }\n";

        rendererStrictUnclosedCommentTagTrue.openDocument();
        rendererStrictUnclosedCommentTagTrue.openTag("script", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagTrue.text(js);
        rendererStrictUnclosedCommentTagTrue.closeTag("script");
        rendererStrictUnclosedCommentTagTrue.closeDocument();

        // We could actually allow this since --> is not banned per 4.12.1.3
        assertEquals("<script></script>", rendered.toString());
        assertEquals("Invalid CDATA text content : -->y) { ..", Joiner.on('\n').join(errors));
        errors.clear();
    }

    public final void testShortHtmlCommentInScript() throws Exception {
        String js = "// <!----> <!--->";

        rendererStrictUnclosedCommentTagTrue.openDocument();
        rendererStrictUnclosedCommentTagTrue.openTag("script", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagTrue.text(js);
        rendererStrictUnclosedCommentTagTrue.closeTag("script");
        rendererStrictUnclosedCommentTagTrue.closeDocument();

        // We could actually allow this since --> is not banned per 4.12.1.3
        assertEquals("<script></script>", rendered.toString());
        assertEquals("Invalid CDATA text content : <!--->", Joiner.on('\n').join(errors));
        errors.clear();
    }

    public final void testHtml51SemanticsScriptingExample5Part3() throws Exception {
        String js = "<!-- if ( player<script ) { ... } -->";

        rendererStrictUnclosedCommentTagTrue.openDocument();
        rendererStrictUnclosedCommentTagTrue.openTag("script", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagTrue.text(js);
        rendererStrictUnclosedCommentTagTrue.closeTag("script");
        rendererStrictUnclosedCommentTagTrue.closeDocument();

        assertEquals("<script></script>", rendered.toString());
        assertEquals("Invalid CDATA text content : <script ) ", Joiner.on('\n').join(errors));
        errors.clear();
    }

    public final void testHtml51SemanticsScriptingExample5Part4() throws Exception {
        String js = "<!--\n" + "if (x < !--y) { ... }\n" + "if (!--y > x) { ... }\n" + "if (!(--y) > x) { ... }\n"
                + "if (player < script) { ... }\n" + "if (script > player) { ... }\n" + "-->";

        rendererStrictUnclosedCommentTagFalse.openDocument();
        rendererStrictUnclosedCommentTagFalse.openTag("script", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.text(js);
        rendererStrictUnclosedCommentTagFalse.closeTag("script");
        rendererStrictUnclosedCommentTagFalse.closeDocument();

        assertEquals(
                "<script><!--\n" + "if (x < !--y) { ... }\n" + "if (!--y > x) { ... }\n" + "if (!(--y) > x) { ... }\n"
                        + "if (player < script) { ... }\n" + "if (script > player) { ... }\n" + "--></script>",
                rendered.toString());
    }

    public final void testHtmlCommentInRcdata() throws Exception {
        String str = "// <!----> <!---> <!--";

        rendererStrictUnclosedCommentTagFalse.openDocument();
        rendererStrictUnclosedCommentTagFalse.openTag("title", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.text(str);
        rendererStrictUnclosedCommentTagFalse.closeTag("title");
        rendererStrictUnclosedCommentTagFalse.openTag("textarea", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.text(str);
        rendererStrictUnclosedCommentTagFalse.closeTag("textarea");
        rendererStrictUnclosedCommentTagFalse.closeDocument();

        assertEquals("<title>// &lt;!----&gt; &lt;!---&gt; &lt;!--</title>"
                + "<textarea>// &lt;!----&gt; &lt;!---&gt; &lt;!--</textarea>", rendered.toString());
    }

    public final void testTagInCdata() throws Exception {
        rendererStrictUnclosedCommentTagFalse.openDocument();
        rendererStrictUnclosedCommentTagFalse.openTag("script", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.text("alert('");
        rendererStrictUnclosedCommentTagFalse.openTag("b", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.text("foo");
        rendererStrictUnclosedCommentTagFalse.closeTag("b");
        rendererStrictUnclosedCommentTagFalse.text("')");
        rendererStrictUnclosedCommentTagFalse.closeTag("script");
        rendererStrictUnclosedCommentTagFalse.closeDocument();

        assertEquals("<script>alert('foo')</script>", rendered.toString());
        assertEquals(Joiner.on('\n').join("Tag content cannot appear inside CDATA element : b",
                "Tag content cannot appear inside CDATA element : b"), Joiner.on('\n').join(errors));
        errors.clear();
    }

    public final void testUnclosedEscapingTextSpan() throws Exception {
        rendererStrictUnclosedCommentTagTrue.openDocument();
        rendererStrictUnclosedCommentTagTrue.openTag("script", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagTrue.text("<!--alert('</script>')");
        rendererStrictUnclosedCommentTagTrue.closeTag("script");
        rendererStrictUnclosedCommentTagTrue.closeDocument();

        assertEquals("<script></script>", rendered.toString());
        assertEquals("Invalid CDATA text content : </script>'", Joiner.on('\n').join(errors));
        errors.clear();
    }

    public final void testAlmostCompleteEndTag() throws Exception {
        rendererStrictUnclosedCommentTagFalse.openDocument();
        rendererStrictUnclosedCommentTagFalse.openTag("script", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.text("//</scrip");
        rendererStrictUnclosedCommentTagFalse.closeTag("script");
        rendererStrictUnclosedCommentTagFalse.closeDocument();

        assertEquals("<script>//</scrip</script>", rendered.toString());
    }

    public final void testBalancedCommentInNoscript() throws Exception {
        rendererStrictUnclosedCommentTagFalse.openDocument();
        rendererStrictUnclosedCommentTagFalse.openTag("noscript", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.text("<!--<script>foo</script>-->");
        rendererStrictUnclosedCommentTagFalse.closeTag("noscript");
        rendererStrictUnclosedCommentTagFalse.closeDocument();

        assertEquals("<noscript>&lt;!--&lt;script&gt;foo&lt;/script&gt;--&gt;</noscript>", rendered.toString());
    }

    public final void testUnbalancedCommentInNoscript() throws Exception {
        rendererStrictUnclosedCommentTagFalse.openDocument();
        rendererStrictUnclosedCommentTagFalse.openTag("noscript", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.text("<!--<script>foo</script>--");
        rendererStrictUnclosedCommentTagFalse.closeTag("noscript");
        rendererStrictUnclosedCommentTagFalse.openTag("noscript", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.text("<script>foo</script>-->");
        rendererStrictUnclosedCommentTagFalse.closeTag("noscript");
        rendererStrictUnclosedCommentTagFalse.closeDocument();

        assertEquals("<noscript>&lt;!--&lt;script&gt;foo&lt;/script&gt;--</noscript>"
                + "<noscript>&lt;script&gt;foo&lt;/script&gt;--&gt;</noscript>", rendered.toString());
    }

    public final void testSupplementaryCodepoints() throws Exception {
        rendererStrictUnclosedCommentTagFalse.openDocument();
        rendererStrictUnclosedCommentTagFalse.text("\uD87E\uDC1A"); // Supplementary codepoint U+2F81A
        rendererStrictUnclosedCommentTagFalse.closeDocument();

        assertEquals("&#x2f81a;", rendered.toString());
    }

    // Test that policies that naively allow <xmp>, <listing>, or <plaintext>
    // on XHTML don't shoot themselves in the foot.

    public final void testPreSubstitutes1() throws Exception {
        rendererStrictUnclosedCommentTagFalse.openDocument();
        rendererStrictUnclosedCommentTagFalse.openTag("Xmp", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.text("<form>Hello, World</form>");
        rendererStrictUnclosedCommentTagFalse.closeTag("Xmp");
        rendererStrictUnclosedCommentTagFalse.closeDocument();

        assertEquals("<pre>&lt;form&gt;Hello, World&lt;/form&gt;</pre>", rendered.toString());
    }

    public final void testPreSubstitutes2() throws Exception {
        rendererStrictUnclosedCommentTagFalse.openDocument();
        rendererStrictUnclosedCommentTagFalse.openTag("xmp", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.text("<form>Hello, World</form>");
        rendererStrictUnclosedCommentTagFalse.closeTag("xmp");
        rendererStrictUnclosedCommentTagFalse.closeDocument();

        assertEquals("<pre>&lt;form&gt;Hello, World&lt;/form&gt;</pre>", rendered.toString());
    }

    public final void testPreSubstitutes3() throws Exception {
        rendererStrictUnclosedCommentTagFalse.openDocument();
        rendererStrictUnclosedCommentTagFalse.openTag("LISTING", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.text("<form>Hello, World</form>");
        rendererStrictUnclosedCommentTagFalse.closeTag("LISTING");
        rendererStrictUnclosedCommentTagFalse.closeDocument();

        assertEquals("<pre>&lt;form&gt;Hello, World&lt;/form&gt;</pre>", rendered.toString());
    }

    public final void testPreSubstitutes4() throws Exception {
        rendererStrictUnclosedCommentTagFalse.openDocument();
        rendererStrictUnclosedCommentTagFalse.openTag("plaintext", ImmutableList.<String>of());
        rendererStrictUnclosedCommentTagFalse.text("<form>Hello, World</form>");
        rendererStrictUnclosedCommentTagFalse.closeDocument();

        assertEquals("<pre>&lt;form&gt;Hello, World&lt;/form&gt;", rendered.toString());
    }
}
