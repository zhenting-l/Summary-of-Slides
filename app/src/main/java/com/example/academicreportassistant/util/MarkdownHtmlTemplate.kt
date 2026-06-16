package com.lzt.summaryofslides.util

import android.content.Context
import java.io.File

object MarkdownHtmlTemplate {
    fun wrap(markdown: String): String {
        val safe = htmlEscape(markdown)
            .replace("</pre", "<\\/pre")
            .replace("</script", "<\\/script")
            .replace("</style", "<\\/style")
        val dollar = "${'$'}"
        return """
<!doctype html>
<html>
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <link rel="stylesheet" href="file:///android_asset/katex/katex.min.css" />
    <style>
      body { margin: 0; background: #fff; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; }
      .container { padding: 16px; }
      .markdown-body { box-sizing: border-box; min-width: 200px; max-width: 980px; margin: 0 auto; padding: 16px; }
      .toc { max-width: 980px; margin: 0 auto; padding: 0 16px; }
      .toc details { border: 1px solid #e5e7eb; border-radius: 10px; padding: 10px 12px; background: #fafafa; }
      .toc summary { cursor: pointer; font-weight: 600; }
      .toc a { text-decoration: none; }
      .toc ul { margin: 10px 0 0 18px; padding: 0; }
      .toc li { margin: 6px 0; }
      img { max-width: 100%; height: auto; }
      #status { position: fixed; top: 8px; right: 8px; background: #fff3cd; color: #856404; padding: 6px 10px; border-radius: 6px; font-size: 11px; border: 1px solid #ffeeba; max-width: 70%; z-index: 9999; word-break: break-all; }
      #status.ok { background: #d4edda; color: #155724; border-color: #c3e6cb; }
      #status.err { background: #f8d7da; color: #721c24; border-color: #f5c6cb; }
    </style>
  </head>
  <body>
    <div id="status" style="display:none"></div>
    <div class="container">
      <div class="toc" id="toc"></div>
      <article id="content" class="markdown-body"></article>
    </div>
    <pre id="md-source" style="display:none">$safe</pre>
    <script src="file:///android_asset/marked.min.js" onerror="window.__assetError='marked'"></script>
    <script src="file:///android_asset/katex/katex.min.js" onerror="window.__assetError='katex'"></script>
    <script src="file:///android_asset/katex/auto-render.min.js" onerror="window.__assetError='auto-render'"></script>
    <script>
      (function () {
        function setStatus(msg, kind) {
          var el = document.getElementById("status");
          if (!el) return;
          el.className = kind || "";
          el.textContent = msg;
          el.style.display = "block";
        }
        function hasOddBackslashes(text, index) {
          var count = 0;
          for (var i = index - 1; i >= 0 && text.charAt(i) === "\\"; i--) count++;
          return (count % 2) === 1;
        }
        function protectMathForMarked(input) {
          function encodeMathChar(ch) {
            switch (ch) {
              case "_": return "\uE000";
              case "*": return "\uE001";
              case "[": return "\uE002";
              case "]": return "\uE003";
              default: return null;
            }
          }
          function encodeEscaped(ch) {
            switch (ch) {
              case "\\": return "\uE010";
              case "`": return "\uE011";
              case "*": return "\uE012";
              case "_": return "\uE013";
              case "{": return "\uE014";
              case "}": return "\uE015";
              case "[": return "\uE016";
              case "]": return "\uE017";
              case "(": return "\uE018";
              case ")": return "\uE019";
              case "#": return "\uE01A";
              case "+": return "\uE01B";
              case "-": return "\uE01C";
              case ".": return "\uE01D";
              case "!": return "\uE01E";
              case ">": return "\uE01F";
              case "|": return "\uE020";
              default: return null;
            }
          }
          var out = "";
          var i = 0;
          var inInlineMath = false;
          var inBlockMath = false;
          var inCodeFence = false;
          var inInlineCode = false;
          while (i < input.length) {
            if (!inInlineMath && !inBlockMath) {
              if (!inInlineCode && input.substr(i, 3) === "```") {
                inCodeFence = !inCodeFence;
                out += "```";
                i += 3;
                continue;
              }
              if (!inCodeFence && input.charAt(i) === "`") {
                inInlineCode = !inInlineCode;
                out += "`";
                i += 1;
                continue;
              }
            }
            if (!inCodeFence && !inInlineCode) {
              var ch = input.charAt(i);
              if (ch === "$" && !hasOddBackslashes(input, i)) {
                var isDoubleDollar = input.charAt(i + 1) === "$";
                if (!inInlineMath && !inBlockMath) {
                  if (isDoubleDollar) {
                    inBlockMath = true;
                    out += "$$";
                    i += 2;
                    continue;
                  }
                  inInlineMath = true;
                  out += "$";
                  i += 1;
                  continue;
                }
                if (inBlockMath && isDoubleDollar) {
                  inBlockMath = false;
                  out += "$$";
                  i += 2;
                  continue;
                }
                if (inInlineMath && !isDoubleDollar) {
                  inInlineMath = false;
                  out += "$";
                  i += 1;
                  continue;
                }
              }
            }
            if (inInlineMath || inBlockMath) {
              if (input.charAt(i) === "\\" && i + 1 < input.length) {
                var encodedEscape = encodeEscaped(input.charAt(i + 1));
                if (encodedEscape !== null) {
                  out += encodedEscape;
                  i += 2;
                  continue;
                }
              }
              var encodedMathChar = encodeMathChar(input.charAt(i));
              if (encodedMathChar !== null) {
                out += encodedMathChar;
                i += 1;
                continue;
              }
            }
            out += input.charAt(i);
            i += 1;
          }
          return out;
        }
        function restoreMathFromMarked(text) {
          return text
            .replace(/\uE000/g, "_")
            .replace(/\uE001/g, "*")
            .replace(/\uE002/g, "[")
            .replace(/\uE003/g, "]")
            .replace(/\uE010/g, "\\\\")
            .replace(/\uE011/g, "\\`")
            .replace(/\uE012/g, "\\*")
            .replace(/\uE013/g, "\\_")
            .replace(/\uE014/g, "\\{")
            .replace(/\uE015/g, "\\}")
            .replace(/\uE016/g, "\\[")
            .replace(/\uE017/g, "\\]")
            .replace(/\uE018/g, "\\(")
            .replace(/\uE019/g, "\\)")
            .replace(/\uE01A/g, "\\#")
            .replace(/\uE01B/g, "\\+")
            .replace(/\uE01C/g, "\\-")
            .replace(/\uE01D/g, "\\.")
            .replace(/\uE01E/g, "\\!")
            .replace(/\uE01F/g, "\\>")
            .replace(/\uE020/g, "\\|");
        }
        window.addEventListener("error", function (e) {
          var src = e && e.target && (e.target.src || e.target.href);
          setStatus("资源加载失败: " + (src || e.message || "未知"), "err");
        }, true);

        var srcEl = document.getElementById("md-source");
        var md = srcEl ? (srcEl.textContent || "") : "";
        md = md.replace(/\t/g, "\\t");
        // 只在真正的数学环境内保护字符，避免误伤两个公式之间的普通文本。
        md = protectMathForMarked(md);
        var target = document.getElementById("content");
        if (!target) { setStatus("找不到渲染目标元素", "err"); return; }

        var markedOk = window.marked && typeof window.marked.parse === "function";
        var renderMathOk = typeof renderMathInElement === "function";
        if (window.__assetError) {
          setStatus("脚本加载失败: " + window.__assetError, "err");
        }
        if (!markedOk) {
          target.textContent = md;
          setStatus("marked.js 未加载，原文输出", "err");
          return;
        }

        var html = "";
        try {
          window.marked.setOptions({ gfm: true, breaks: false, mangle: false, headerIds: false });
          html = window.marked.parse(md);
          target.innerHTML = html;
          // marked 之后还原公式中的占位符，让 KaTeX 拿到原始 LaTeX。
          try {
            var walker = document.createTreeWalker(target, NodeFilter.SHOW_TEXT, null, false);
            var tn;
            while ((tn = walker.nextNode())) {
              tn.nodeValue = restoreMathFromMarked(tn.nodeValue);
            }
          } catch (e) {}
        } catch (e) {
          target.textContent = md;
          setStatus("marked.parse 失败: " + e.message, "err");
          return;
        }

        try {
          var headings = target.querySelectorAll("h1, h2, h3");
          if (headings.length) {
            var ul = document.createElement("ul");
            for (var i = 0; i < headings.length; i++) {
              var h = headings[i];
              if (!h.id) {
                var id = (h.textContent || "").trim().toLowerCase()
                  .replace(/[^a-z0-9\u4e00-\u9fa5\s-]/g, "")
                  .replace(/\s+/g, "-")
                  .substring(0, 64);
                if (!id) id = "h-" + i;
                h.id = id;
              }
              var li = document.createElement("li");
              if (h.tagName === "H2") li.style.marginLeft = "10px";
              if (h.tagName === "H3") li.style.marginLeft = "20px";
              var a = document.createElement("a");
              a.href = "#" + h.id;
              a.textContent = h.textContent;
              li.appendChild(a);
              ul.appendChild(li);
            }
            var details = document.createElement("details");
            details.open = false;
            var summary = document.createElement("summary");
            summary.textContent = "目录";
            details.appendChild(summary);
            details.appendChild(ul);
            var toc = document.getElementById("toc");
            if (toc) toc.appendChild(details);
          }
        } catch (e) {}

        if (renderMathOk) {
          try {
            renderMathInElement(target, {
              delimiters: [
                { left: "${dollar}${dollar}", right: "${dollar}${dollar}", display: true },
                { left: "${dollar}", right: "${dollar}", display: false }
              ],
              throwOnError: false
            });
          } catch (e) {
            setStatus("KaTeX 渲染失败: " + e.message, "err");
          }
        } else {
          setStatus("KaTeX auto-render 未加载，数学公式未渲染", "err");
        }

        if (markedOk && renderMathOk) {
          setStatus("渲染完成", "ok");
        }
      })();
    </script>
  </body>
</html>
""".trimIndent()
    }

    fun wrapStandalone(markdown: String, context: Context): String {
        val markedJs = readAssetText(context, "marked.min.js")
        val katexJs = readAssetText(context, "katex/katex.min.js")
        val katexCss = readAssetText(context, "katex/katex.min.css")
        val autoRenderJs = readAssetText(context, "katex/auto-render.min.js")
        val cssWithFonts = embedFontsInCss(context, katexCss)

        val safe = htmlEscape(markdown)
            .replace("</pre", "<\\/pre")
            .replace("</style", "<\\/style")
        val dollar = "${'$'}"

        val safeMarkedJs = markedJs.replace("</script", "<\\/script")
        val safeKatexJs = katexJs.replace("</script", "<\\/script")
        val safeAutoRenderJs = autoRenderJs.replace("</script", "<\\/script")

        return """
<!doctype html>
<html>
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <style>
$cssWithFonts
body { margin: 0; background: #fff; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; }
.container { padding: 16px; }
.markdown-body { box-sizing: border-box; min-width: 200px; max-width: 980px; margin: 0 auto; padding: 16px; }
.toc { max-width: 980px; margin: 0 auto; padding: 0 16px; }
.toc details { border: 1px solid #e5e7eb; border-radius: 10px; padding: 10px 12px; background: #fafafa; }
.toc summary { cursor: pointer; font-weight: 600; }
.toc a { text-decoration: none; }
.toc ul { margin: 10px 0 0 18px; padding: 0; }
.toc li { margin: 6px 0; }
img { max-width: 100%; height: auto; }
#status { position: fixed; top: 8px; right: 8px; background: #fff3cd; color: #856404; padding: 6px 10px; border-radius: 6px; font-size: 11px; border: 1px solid #ffeeba; max-width: 70%; z-index: 9999; word-break: break-all; }
#status.ok { background: #d4edda; color: #155724; border-color: #c3e6cb; }
#status.err { background: #f8d7da; color: #721c24; border-color: #f5c6cb; }
    </style>
  </head>
  <body>
    <div id="status" style="display:none"></div>
    <div class="container">
      <div class="toc" id="toc"></div>
      <article id="content" class="markdown-body"></article>
    </div>
    <pre id="md-source" style="display:none">$safe</pre>
    <script>
$safeMarkedJs
    </script>
    <script>
$safeKatexJs
    </script>
    <script>
$safeAutoRenderJs
    </script>
    <script>
      (function () {
        function setStatus(msg, kind) {
          var el = document.getElementById("status");
          if (!el) return;
          el.className = kind || "";
          el.textContent = msg;
          el.style.display = "block";
        }
        function hasOddBackslashes(text, index) {
          var count = 0;
          for (var i = index - 1; i >= 0 && text.charAt(i) === "\\"; i--) count++;
          return (count % 2) === 1;
        }
        function protectMathForMarked(input) {
          function encodeMathChar(ch) {
            switch (ch) {
              case "_": return "\uE000";
              case "*": return "\uE001";
              case "[": return "\uE002";
              case "]": return "\uE003";
              default: return null;
            }
          }
          function encodeEscaped(ch) {
            switch (ch) {
              case "\\": return "\uE010";
              case "`": return "\uE011";
              case "*": return "\uE012";
              case "_": return "\uE013";
              case "{": return "\uE014";
              case "}": return "\uE015";
              case "[": return "\uE016";
              case "]": return "\uE017";
              case "(": return "\uE018";
              case ")": return "\uE019";
              case "#": return "\uE01A";
              case "+": return "\uE01B";
              case "-": return "\uE01C";
              case ".": return "\uE01D";
              case "!": return "\uE01E";
              case ">": return "\uE01F";
              case "|": return "\uE020";
              default: return null;
            }
          }
          var out = "";
          var i = 0;
          var inInlineMath = false;
          var inBlockMath = false;
          var inCodeFence = false;
          var inInlineCode = false;
          while (i < input.length) {
            if (!inInlineMath && !inBlockMath) {
              if (!inInlineCode && input.substr(i, 3) === "```") {
                inCodeFence = !inCodeFence;
                out += "```";
                i += 3;
                continue;
              }
              if (!inCodeFence && input.charAt(i) === "`") {
                inInlineCode = !inInlineCode;
                out += "`";
                i += 1;
                continue;
              }
            }
            if (!inCodeFence && !inInlineCode) {
              var ch = input.charAt(i);
              if (ch === "$" && !hasOddBackslashes(input, i)) {
                var isDoubleDollar = input.charAt(i + 1) === "$";
                if (!inInlineMath && !inBlockMath) {
                  if (isDoubleDollar) {
                    inBlockMath = true;
                    out += "$$";
                    i += 2;
                    continue;
                  }
                  inInlineMath = true;
                  out += "$";
                  i += 1;
                  continue;
                }
                if (inBlockMath && isDoubleDollar) {
                  inBlockMath = false;
                  out += "$$";
                  i += 2;
                  continue;
                }
                if (inInlineMath && !isDoubleDollar) {
                  inInlineMath = false;
                  out += "$";
                  i += 1;
                  continue;
                }
              }
            }
            if (inInlineMath || inBlockMath) {
              if (input.charAt(i) === "\\" && i + 1 < input.length) {
                var encodedEscape = encodeEscaped(input.charAt(i + 1));
                if (encodedEscape !== null) {
                  out += encodedEscape;
                  i += 2;
                  continue;
                }
              }
              var encodedMathChar = encodeMathChar(input.charAt(i));
              if (encodedMathChar !== null) {
                out += encodedMathChar;
                i += 1;
                continue;
              }
            }
            out += input.charAt(i);
            i += 1;
          }
          return out;
        }
        function restoreMathFromMarked(text) {
          return text
            .replace(/\uE000/g, "_")
            .replace(/\uE001/g, "*")
            .replace(/\uE002/g, "[")
            .replace(/\uE003/g, "]")
            .replace(/\uE010/g, "\\\\")
            .replace(/\uE011/g, "\\`")
            .replace(/\uE012/g, "\\*")
            .replace(/\uE013/g, "\\_")
            .replace(/\uE014/g, "\\{")
            .replace(/\uE015/g, "\\}")
            .replace(/\uE016/g, "\\[")
            .replace(/\uE017/g, "\\]")
            .replace(/\uE018/g, "\\(")
            .replace(/\uE019/g, "\\)")
            .replace(/\uE01A/g, "\\#")
            .replace(/\uE01B/g, "\\+")
            .replace(/\uE01C/g, "\\-")
            .replace(/\uE01D/g, "\\.")
            .replace(/\uE01E/g, "\\!")
            .replace(/\uE01F/g, "\\>")
            .replace(/\uE020/g, "\\|");
        }

        var srcEl = document.getElementById("md-source");
        var md = srcEl ? (srcEl.textContent || "") : "";
        md = md.replace(/\t/g, "\\t");
        // 只在真正的数学环境内保护字符，避免误伤两个公式之间的普通文本。
        md = protectMathForMarked(md);
        var target = document.getElementById("content");
        if (!target) { setStatus("找不到渲染目标元素", "err"); return; }

        var markedOk = window.marked && typeof window.marked.parse === "function";
        var renderMathOk = typeof renderMathInElement === "function";
        if (!markedOk) {
          target.textContent = md;
          setStatus("marked.js 未加载，原文输出", "err");
          return;
        }

        var html = "";
        try {
          window.marked.setOptions({ gfm: true, breaks: false, mangle: false, headerIds: false });
          html = window.marked.parse(md);
          target.innerHTML = html;
          // marked 之后还原公式中的占位符，让 KaTeX 拿到原始 LaTeX。
          try {
            var walker = document.createTreeWalker(target, NodeFilter.SHOW_TEXT, null, false);
            var tn;
            while ((tn = walker.nextNode())) {
              tn.nodeValue = restoreMathFromMarked(tn.nodeValue);
            }
          } catch (e) {}
        } catch (e) {
          target.textContent = md;
          setStatus("marked.parse 失败: " + e.message, "err");
          return;
        }

        try {
          var headings = target.querySelectorAll("h1, h2, h3");
          if (headings.length) {
            var ul = document.createElement("ul");
            for (var i = 0; i < headings.length; i++) {
              var h = headings[i];
              if (!h.id) {
                var id = (h.textContent || "").trim().toLowerCase()
                  .replace(/[^a-z0-9\u4e00-\u9fa5\s-]/g, "")
                  .replace(/\s+/g, "-")
                  .substring(0, 64);
                if (!id) id = "h-" + i;
                h.id = id;
              }
              var li = document.createElement("li");
              if (h.tagName === "H2") li.style.marginLeft = "10px";
              if (h.tagName === "H3") li.style.marginLeft = "20px";
              var a = document.createElement("a");
              a.href = "#" + h.id;
              a.textContent = h.textContent;
              li.appendChild(a);
              ul.appendChild(li);
            }
            var details = document.createElement("details");
            details.open = false;
            var summary = document.createElement("summary");
            summary.textContent = "目录";
            details.appendChild(summary);
            details.appendChild(ul);
            var toc = document.getElementById("toc");
            if (toc) toc.appendChild(details);
          }
        } catch (e) {}

        if (renderMathOk) {
          try {
            renderMathInElement(target, {
              delimiters: [
                { left: "${dollar}${dollar}", right: "${dollar}${dollar}", display: true },
                { left: "${dollar}", right: "${dollar}", display: false }
              ],
              throwOnError: false
            });
          } catch (e) {
            setStatus("KaTeX 渲染失败: " + e.message, "err");
          }
        } else {
          setStatus("KaTeX auto-render 未加载，数学公式未渲染", "err");
        }

        if (markedOk && renderMathOk) {
          setStatus("渲染完成", "ok");
        }
      })();
    </script>
  </body>
</html>
""".trimIndent()
    }

    fun writeToCache(context: Context, markdown: String, tag: String = "summary"): File {
        val dir = File(context.cacheDir, "mdview").apply { mkdirs() }
        val safe = markdown.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(40)
        val file = File(dir, "${tag}-${safe}.html")
        file.writeText(wrap(markdown), Charsets.UTF_8)
        return file
    }

    private fun readAssetText(context: Context, path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun embedFontsInCss(context: Context, css: String): String {
        val pattern = Regex("""url\(\s*['"]?fonts/(KaTeX_[A-Za-z0-9_-]+\.woff2)['"]?\s*\)""")
        return pattern.replace(css) { match ->
            val fontFile = match.groupValues[1]
            val bytes = context.assets.open("katex/fonts/$fontFile").use { it.readBytes() }
            val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            "url(data:font/woff2;base64,$b64)"
        }
    }

    private fun htmlEscape(s: String): String {
        if (s.isEmpty()) return s
        val out = StringBuilder(s.length)
        for (c in s) {
            when (c) {
                '&' -> out.append("&amp;")
                '<' -> out.append("&lt;")
                '>' -> out.append("&gt;")
                else -> out.append(c)
            }
        }
        return out.toString()
    }
}
