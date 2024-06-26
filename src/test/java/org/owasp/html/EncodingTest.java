// Copyright (c) 2012, Mike Samuel
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

import org.junit.Test;

import junit.framework.TestCase;

@SuppressWarnings("javadoc")
public final class EncodingTest extends TestCase {
  private static void assertDecodedHtml(String want, String inputHtml) {
    assertDecodedHtml(want, want, inputHtml);
  }

  private static void assertDecodedHtml(
          String wantText, String wantAttr, String inputHtml
  ) {
    assertEquals(
            "!inAttribute: " + inputHtml,
            wantText,
            Encoding.decodeHtml(inputHtml, false)
    );
    assertEquals(
            "inAttribute: " + inputHtml,
            wantAttr,
            Encoding.decodeHtml(inputHtml, true)
    );
  }

  @Test
  public static final void testDecodeHtml() {
    String html =
      "The quick&nbsp;brown fox&#xa;jumps over&#xd;&#10;the lazy dog&#x000a;";
    //          1         2         3         4         5         6
    // 123456789012345678901234567890123456789012345678901234567890123456789
    String golden =
      "The quick\u00a0brown fox\njumps over\r\nthe lazy dog\n";
    assertDecodedHtml(golden, html);

    // Don't allocate a new string when no entities.
    assertSame(golden, golden);

    // test interrupted escapes and escapes at end of file handled gracefully
    assertDecodedHtml("\\\\u000a", "\\\\u000a");
    assertDecodedHtml("\n", "&#x000a;");
    assertDecodedHtml("\n", "&#x00a;");
    assertDecodedHtml("\n", "&#x0a;");
    assertDecodedHtml("\n", "&#xa;");
    assertDecodedHtml(
        String.valueOf(Character.toChars(0x10000)),
            "&#x10000;"
    );
    assertDecodedHtml("\n", "&#xa");
    assertDecodedHtml("&#x00ziggy", "&#x00ziggy");
    assertDecodedHtml("&#xa00z;", "&#xa00z;");
    assertDecodedHtml("&#\n", "&#&#x000a;");
    assertDecodedHtml("&#x\n", "&#x&#x000a;");
    assertDecodedHtml("\n\n", "&#xa&#x000a;");
    assertDecodedHtml("&#\n", "&#&#xa;");
    assertDecodedHtml("&#x", "&#x");
    assertDecodedHtml("", "&#x0"); // NUL elided.
    assertDecodedHtml("&#", "&#");

    assertDecodedHtml("\\", "\\");
    assertDecodedHtml("&", "&");

    assertDecodedHtml("&#000a;", "&#000a;");
    assertDecodedHtml("\n", "&#10;");
    assertDecodedHtml("\n", "&#010;");
    assertDecodedHtml("\n", "&#0010;");
    assertDecodedHtml("\t", "&#9;");
    assertDecodedHtml("\n", "&#10");
    assertDecodedHtml("&#00ziggy", "&#00ziggy");
    assertDecodedHtml("&#\n", "&#&#010;");
    assertDecodedHtml("\n", "&#0&#010;");
    assertDecodedHtml("\n", "&#01&#10;");
    assertDecodedHtml("&#\n", "&#&#10;");
    assertDecodedHtml("", "&#1"); // Invalid XML char elided.
    assertDecodedHtml("\t", "&#9");
    assertDecodedHtml("\n", "&#10");

    // test the named escapes
    assertDecodedHtml("<", "&lt;");
    assertDecodedHtml(">", "&gt;");
    assertDecodedHtml("\"", "&quot;");
    assertDecodedHtml("'", "&apos;");
    assertDecodedHtml("'", "&#39;");
    assertDecodedHtml("'", "&#x27;");
    assertDecodedHtml("&", "&amp;");
    assertDecodedHtml("&lt;", "&amp;lt;");
    assertDecodedHtml("&", "&AMP;");
    assertDecodedHtml("&", "&AMP");
    assertDecodedHtml("&", "&AmP;");
    assertDecodedHtml("\u0391", "&Alpha;");
    assertDecodedHtml("\u03b1", "&alpha;");
    // U+1D49C requires a surrogate pair in UTF-16.
    assertDecodedHtml("\ud835\udc9c", "&Ascr;");
    // &fjlig; refers to 2 characters.
    assertDecodedHtml("fj", "&fjlig;");
    // HTML entity with the longest name.
    assertDecodedHtml("\u2233", "&CounterClockwiseContourIntegral;");
    // Missing the semicolon.
    assertDecodedHtml(
            "&CounterClockwiseContourIntegral",
            "&CounterClockwiseContourIntegral"
    );

    assertDecodedHtml("&;", "&;");
    assertDecodedHtml("&bogus;", "&bogus;");

    // Some strings decode differently depending on whether or not they're in an HTML attribute.
    assertDecodedHtml(
            "?foo\u00B6m=bar",
            "?foo&param=bar",
            "?foo&param=bar"
    );
    assertDecodedHtml(
            "?foo\u00B6=bar",
            "?foo&para=bar",
            "?foo&para=bar"
    );
  }

  @Test
  public static final void testAppendNumericEntityAndEncodeOnto()
      throws Exception {
    StringBuilder sb = new StringBuilder();
    StringBuilder cps = new StringBuilder();
    for (int codepoint : new int[] {
        0, 9, '\n', '@', 0x80, 0xff, 0x100, 0xfff, 0x1000, 0x123a, 0xffff,
        0x10000, Character.MAX_CODE_POINT }) {
      Encoding.appendNumericEntity(codepoint, sb);
      sb.append(' ');

      cps.appendCodePoint(codepoint).append(' ');
    }

    assertEquals(
         "&#0; &#9; &#10; &#64; &#x80; &#xff; &#x100; &#xfff; &#x1000; "
         + "&#x123a; &#xffff; &#x10000; &#x10ffff; ",
         sb.toString());

    StringBuilder out = new StringBuilder();
    Encoding.encodeHtmlAttribOnto(cps.toString(), out);
    assertEquals(
        " \t \n &#64; \u0080 \u00ff \u0100 \u0fff \u1000 "
        + "\u123a  &#x10000; &#x10ffff; ",
        out.toString());
  }

  @Test
  public static final void testAngularJsBracesInTextNode() throws Exception {
    StringBuilder sb = new StringBuilder();

    Encoding.encodePcdataOnto("{{angularVariable}}", sb);
    assertEquals("{<!-- -->{angularVariable}}", sb.toString());

    sb.setLength(0);

    Encoding.encodePcdataOnto("{", sb);
    Encoding.encodePcdataOnto("{angularVariable}}", sb);
    assertEquals("{<!-- -->{angularVariable}}", sb.toString());
  }

  private static final void assertStripped(String stripped, String orig) {
    String actual = Encoding.stripBannedCodeunits(orig);
    assertEquals(orig, stripped, actual);
    if (stripped.equals(orig)) {
      assertSame(actual, orig);
    }

    StringBuilder sb = new StringBuilder(orig);
    Encoding.stripBannedCodeunits(sb);
    assertEquals(orig, stripped, sb.toString());
  }

  @Test
  public static final void testStripBannedCodeunits() {
    assertStripped("", "");
    assertStripped("foo", "foo");
    assertStripped("foobar", "foo\u0000bar");
    assertStripped("foobar", "foo\u0000bar\u0000");
    assertStripped("foobar", "foo\ufffebar\u0008");
    assertStripped("foobar", "foo\ud800bar\udc00");
    assertStripped("foo\ud800\udc00bar", "foo\ud800\ud800\udc00bar");
    assertStripped("foo\ud800\udc00bar", "foo\ud800\udc00\ud800bar");
    assertStripped("foo\ud800\udc00bar", "foo\ud800\udc00\udc00bar");
    assertStripped("foo\ud800\udc00bar", "foo\udc00\ud800\udc00bar");
    assertStripped("foo\ud834\udd1ebar", "foo\ud834\udd1ebar");
    assertStripped("foo\ud834\udd1e", "foo\ud834\udd1e");
    assertStripped("\uffef\ufffd", "\uffef\ufffd\ufffe\uffff");
  }

  @Test
  public static final
  void testBadlyDonePostProcessingWillnotAllowInsertingNonceAttributes()
  throws Exception {
    // Some clients do ad-hoc post processing of the output.
    // String replace of {{...}} shouldn't turn
    //   <span title="{{">}} <br class="a nonce=xyz "></span>
    // into
    //   <span title="x <br class="a nonce=xyz "></span>
    // which contains CSP directives.
    // We prevent this by being strict about quotes to prevent ending an
    // attribute with quotes about strict mode, and being strict about equals
    // signs to prevent text nodes or attribute values from introducing an
    // attribute with a value.
    StringBuilder pcdata = new StringBuilder();
    Encoding.encodePcdataOnto("\" nonce=xyz", pcdata);
    assertEquals("&#34; nonce&#61;xyz", pcdata.toString());

    StringBuilder rcdata = new StringBuilder();
    Encoding.encodeRcdataOnto("\" nonce=xyz", rcdata);
    assertEquals("&#34; nonce&#61;xyz", rcdata.toString());

    StringBuilder attrib = new StringBuilder();
    Encoding.encodeHtmlAttribOnto("a nonce=xyz ", attrib);
    assertEquals("a nonce&#61;xyz ", attrib.toString());
  }
}
