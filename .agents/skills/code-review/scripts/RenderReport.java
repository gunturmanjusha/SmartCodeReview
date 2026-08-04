import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RenderReport {
    private static final Pattern LINK = Pattern.compile("\\[([^]]+)]\\(([^)]+)\\)");
    private static final Pattern JACOCO_HTML =
            Pattern.compile("`build/reports/jacoco/test/html/index\\.html`");
    private static final Pattern CODE = Pattern.compile("`([^`]+)`");
    private static final Pattern STRONG = Pattern.compile("\\*\\*([^*]+)\\*\\*");

    private RenderReport() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java RenderReport.java <report.md> <report.html>");
            System.exit(2);
        }

        var markdown = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
        var body = render(markdown);
        var html = """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Smart Code Review</title>
                  <style>
                    :root { color-scheme: light; --ink:#172033; --muted:#5f6b7a; --line:#d8dee8;
                      --panel:#f7f9fc; --blue:#1f5fbf; --navy:#172b4d; }
                    * { box-sizing:border-box; }
                    body { margin:0; background:#edf1f7; color:var(--ink); font:15px/1.55 -apple-system,
                      BlinkMacSystemFont,"Segoe UI",sans-serif; }
                    main { max-width:1500px; margin:32px auto; padding:38px 46px; background:white;
                      border:1px solid var(--line); border-radius:14px; box-shadow:0 10px 30px #24324a18; }
                    h1 { color:var(--navy); font-size:2rem; border-bottom:3px solid var(--blue); padding-bottom:12px; }
                    h2 { color:var(--navy); margin-top:36px; border-bottom:1px solid var(--line); padding-bottom:7px; }
                    h3 { color:#263f68; margin-top:28px; padding:12px 15px; background:#f1f5fb;
                      border-left:5px solid #6b7f9e; border-radius:7px; }
                    h3.sev-blocker { background:#ffe9ec; border-color:#b42336; color:#82162a; }
                    h3.sev-high { background:#fff0e4; border-color:#e56b16; color:#93400b; }
                    h3.sev-medium { background:#fff8d9; border-color:#d5a400; color:#785b00; }
                    h3.sev-low { background:#eaf3ff; border-color:#3478c9; color:#164e8d; }
                    table { width:100%; border-collapse:collapse; margin:16px 0 24px; table-layout:fixed; }
                    th { background:#eaf1fb; color:#17345f; text-align:left; }
                    th,td { border:1px solid var(--line); padding:9px 11px; vertical-align:top;
                      overflow-wrap:anywhere; }
                    tr:nth-child(even) td { background:#fafbfd; }
                    blockquote { margin:18px 0; padding:16px 20px; background:#fff7df;
                      border-left:6px solid #e59b13; border-radius:6px; }
                    blockquote h2 { margin:0 0 8px; border:0; color:#8a4b00; }
                    code { background:#eef2f7; color:#9b2448; padding:2px 5px; border-radius:4px; }
                    pre { padding:16px; overflow:auto; background:#172033; color:#f4f7fb; border-radius:8px; }
                    pre code { color:inherit; background:transparent; padding:0; }
                    a { color:var(--blue); }
                    li { margin:5px 0; }
                    .meta { color:var(--muted); font-size:.9rem; margin-bottom:22px; }
                    @media print { body { background:white; } main { margin:0; border:0; box-shadow:none; max-width:none; } }
                  </style>
                </head>
                <body><main>
                <div class="meta">Generated from <code>latest.md</code>. The Markdown report is the source of truth.</div>
                """ + body + "\n</main></body></html>\n";
        Files.writeString(Path.of(args[1]), html, StandardCharsets.UTF_8);
    }

    private static String render(String markdown) {
        var lines = markdown.lines().toList();
        var out = new StringBuilder();
        boolean inCode = false;
        boolean inList = false;
        boolean inQuote = false;

        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            if (line.startsWith("```")) {
                if (inList) { out.append("</ul>\n"); inList = false; }
                out.append(inCode ? "</code></pre>\n" : "<pre><code>");
                inCode = !inCode;
                continue;
            }
            if (inCode) {
                out.append(escape(line)).append('\n');
                continue;
            }
            if (isTableStart(lines, i)) {
                if (inList) { out.append("</ul>\n"); inList = false; }
                var rows = new ArrayList<List<String>>();
                rows.add(cells(line));
                i += 2;
                while (i < lines.size() && lines.get(i).stripLeading().startsWith("|")) {
                    rows.add(cells(lines.get(i++)));
                }
                i--;
                out.append("<table><thead><tr>");
                rows.getFirst().forEach(cell -> out.append("<th>").append(inline(cell)).append("</th>"));
                out.append("</tr></thead><tbody>\n");
                for (int row = 1; row < rows.size(); row++) {
                    out.append("<tr>");
                    rows.get(row).forEach(cell -> out.append("<td>").append(inline(cell)).append("</td>"));
                    out.append("</tr>\n");
                }
                out.append("</tbody></table>\n");
                continue;
            }
            if (line.startsWith(">")) {
                if (!inQuote) { out.append("<blockquote>\n"); inQuote = true; }
                var content = line.substring(1).stripLeading();
                if (content.startsWith("## ")) out.append("<h2>").append(inline(content.substring(3))).append("</h2>\n");
                else if (!content.isBlank()) out.append("<p>").append(inline(content)).append("</p>\n");
                continue;
            } else if (inQuote) {
                out.append("</blockquote>\n");
                inQuote = false;
            }
            if (line.startsWith("- ")) {
                if (!inList) { out.append("<ul>\n"); inList = true; }
                out.append("<li>").append(inline(line.substring(2))).append("</li>\n");
                continue;
            } else if (inList) {
                out.append("</ul>\n");
                inList = false;
            }
            if (line.startsWith("### ")) {
                var heading = line.substring(4);
                var severityClass = heading.startsWith("🔴") ? "sev-blocker"
                        : heading.startsWith("🟠") ? "sev-high"
                        : heading.startsWith("🟡") ? "sev-medium"
                        : heading.startsWith("🔵") ? "sev-low" : "";
                out.append("<h3 class=\"").append(severityClass).append("\">")
                        .append(inline(heading)).append("</h3>\n");
            }
            else if (line.startsWith("## ")) out.append("<h2>").append(inline(line.substring(3))).append("</h2>\n");
            else if (line.startsWith("# ")) out.append("<h1>").append(inline(line.substring(2))).append("</h1>\n");
            else if (!line.isBlank()) out.append("<p>").append(inline(line)).append("</p>\n");
        }
        if (inList) out.append("</ul>\n");
        if (inQuote) out.append("</blockquote>\n");
        if (inCode) out.append("</code></pre>\n");
        return out.toString();
    }

    private static boolean isTableStart(List<String> lines, int index) {
        return index + 1 < lines.size() && lines.get(index).stripLeading().startsWith("|")
                && lines.get(index + 1).matches("\\s*\\|(?:\\s*:?-+:?\\s*\\|)+\\s*");
    }

    private static List<String> cells(String line) {
        var value = line.strip();
        if (value.startsWith("|")) value = value.substring(1);
        if (value.endsWith("|")) value = value.substring(0, value.length() - 1);
        return List.of(value.split("\\|", -1)).stream().map(String::strip).toList();
    }

    private static String inline(String value) {
        var rendered = escape(value);
        rendered = replace(rendered, LINK, "<a href=\"$2\">$1</a>");
        rendered = replace(rendered, JACOCO_HTML,
                "<a href=\"../jacoco/test/html/index.html\"><strong>Open JaCoCo HTML coverage report</strong></a>");
        rendered = replace(rendered, CODE, "<code>$1</code>");
        return replace(rendered, STRONG, "<strong>$1</strong>");
    }

    private static String replace(String input, Pattern pattern, String replacement) {
        return pattern.matcher(input).replaceAll(replacement);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
