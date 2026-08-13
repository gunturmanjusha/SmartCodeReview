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
    private static final Pattern ISSUE_HEADING = Pattern.compile("^[🔴🟠🟡🔵]\\s+(\\d+)\\.");

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
                  <title>Solution Architect Code Review</title>
                  <style>
                    html { scroll-behavior:smooth; }
                    :root {
                      color-scheme:light;
                      --ink:#172033; --muted:#607087; --line:#d7dfeb; --line-strong:#c4cfdf;
                      --surface:#ffffff; --navy:#142b4a; --blue:#2463c7;
                      --blue-soft:#eaf3ff; --green-soft:#eaf8f0; --yellow-soft:#fff8d9;
                      --orange-soft:#fff0e4; --red-soft:#ffecef; --purple-soft:#f3edff;
                      --slate-soft:#eef2f7; --shadow:0 22px 60px #1d315221,0 3px 10px #1d315214;
                    }
                    * { box-sizing:border-box; }
                    body {
                      margin:0; color:var(--ink);
                      background:radial-gradient(circle at 8% 0%,#dce9ff 0,transparent 28rem),
                        radial-gradient(circle at 92% 8%,#eee5ff 0,transparent 25rem),#edf1f7;
                      font:15px/1.62 Inter,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;
                    }
                    main { max-width:1500px; margin:34px auto; padding:42px 50px 56px; background:var(--surface);
                      border:1px solid #fff; border-radius:18px; box-shadow:var(--shadow); }
                    h1 { margin:0 0 12px; padding:0 0 18px; color:var(--navy); font-size:2.25rem;
                      line-height:1.15; letter-spacing:-.025em; border-bottom:4px solid transparent;
                      border-image:linear-gradient(90deg,var(--blue),#7652bd 58%,#d9e2ee) 1; }
                    h2 { margin:42px 0 16px; padding:10px 14px; color:var(--navy); font-size:1.34rem;
                      line-height:1.25; letter-spacing:-.01em; background:linear-gradient(90deg,#eef4fc,transparent 82%);
                      border-left:4px solid var(--blue); border-bottom:1px solid var(--line); border-radius:6px 0 0 6px; }
                    h3 { margin:30px 0 15px; padding:13px 16px; color:#263f68; font-size:1.08rem;
                      background:linear-gradient(90deg,#edf2f9,#f8fafe); border:1px solid #dbe3ef;
                      border-left:6px solid #6b7f9e; border-radius:9px; box-shadow:0 3px 10px #233a5b0a; }
                    h3.sev-blocker { background:linear-gradient(90deg,#ffe7eb,#fff6f7); border-color:#e8aab5;
                      border-left-color:#b42336; color:#82162a; }
                    h3.sev-high { background:linear-gradient(90deg,#ffebdc,#fff7f0); border-color:#f0bd96;
                      border-left-color:#e56b16; color:#93400b; }
                    h3.sev-medium { background:linear-gradient(90deg,#fff5c9,#fffbed); border-color:#e6d17c;
                      border-left-color:#d5a400; color:#785b00; }
                    h3.sev-low { background:linear-gradient(90deg,#e5f1ff,#f4f9ff); border-color:#afceed;
                      border-left-color:#3478c9; color:#164e8d; }
                    h3[id^="issue-"] { scroll-margin-top:18px; }
                    p { margin:10px 0; }
                    a { color:#165fb9; font-weight:600; text-decoration-color:#8bb5e2; text-underline-offset:2px; }
                    a:hover { color:#0c4386; text-decoration-thickness:2px; }
                    code { padding:2px 5px; color:#8d2850; background:#eef2f7; border:1px solid #e0e6ef;
                      border-radius:4px; font-size:.91em; }
                    pre { padding:18px 20px; overflow:auto; color:#f4f7fb;
                      background:linear-gradient(145deg,#172033,#223451); border:1px solid #344764;
                      border-radius:10px; box-shadow:0 8px 22px #17203320; }
                    pre.code-diff { border-left:6px solid #25a65a; box-shadow:inset 0 0 0 1px #30405d; }
                    pre code { color:inherit; background:transparent; border:0; padding:0; }
                    li { margin:6px 0; }
                    .meta { display:inline-block; margin:0 0 18px; padding:5px 10px; color:var(--muted);
                      background:#f2f5f9; border:1px solid #e0e6ef; border-radius:999px; font-size:.82rem; }
                    blockquote { margin:18px 0 28px; padding:20px 22px; background:#fff7df;
                      border:1px solid #ecd8a0; border-left:7px solid #e59b13; border-radius:10px;
                      box-shadow:0 8px 22px #2f466512; }
                    blockquote h2 { margin:0 0 12px; padding:0; border:0; color:#8a4b00; background:none; }
                    blockquote p { margin:9px 0; }
                    blockquote.decision-pass { background:var(--green-soft); border-color:#99d1af; border-left-color:#25864b; }
                    blockquote.decision-pass h2 { color:#176237; }
                    blockquote.decision-followup { background:var(--yellow-soft); border-color:#e2ca6f; border-left-color:#d5a400; }
                    blockquote.decision-followup h2 { color:#785b00; }
                    blockquote.decision-changes { background:var(--orange-soft); border-color:#edb487; border-left-color:#e56b16; }
                    blockquote.decision-changes h2 { color:#93400b; }
                    blockquote.decision-blocked { background:var(--red-soft); border-color:#e3a1ad; border-left-color:#b42336; }
                    blockquote.decision-blocked h2 { color:#82162a; }
                    blockquote.decision-evidence { background:var(--slate-soft); border-color:#bdc8d7; border-left-color:#6b7f9e; }
                    blockquote.decision-evidence h2 { color:#344967; }
                    blockquote.decision-readiness { padding:18px; background:linear-gradient(135deg,#f7f9fd,#f0f4fa);
                      border-color:#cbd6e5; border-left-color:var(--navy); }
                    blockquote.decision-readiness h2 { padding:0 4px 12px; color:var(--navy); border-bottom:1px solid #cdd7e5; }
                    blockquote.decision-readiness p { padding:10px 12px; background:#fff; border:1px solid #dde4ee;
                      border-left:5px solid #455a77; border-radius:7px; box-shadow:0 2px 7px #233a5b0a; }
                    blockquote.decision-readiness p:nth-of-type(1) { background:var(--orange-soft); border-color:#efc29f;
                      border-left-color:#e56b16; }
                    blockquote.decision-readiness p:nth-of-type(2) { background:var(--purple-soft); border-color:#cdbce9;
                      border-left-color:#7451b9; }
                    blockquote.decision-readiness p:nth-of-type(3) { background:var(--red-soft); border-color:#e5adb6;
                      border-left-color:#b42336; }
                    blockquote.decision-readiness p:nth-of-type(4) { background:#fff4e8; border-color:#efc6a4;
                      border-left-color:#ef7d20; }
                    blockquote.decision-readiness p:nth-of-type(5) { background:var(--green-soft); border-color:#b0d9bf;
                      border-left-color:#25864b; }
                    .executive-cards,.final-recommendation-cards { display:grid;
                      grid-template-columns:repeat(2,minmax(0,1fr)); gap:14px; padding:0; list-style:none; }
                    .executive-cards li,.final-recommendation-cards li { position:relative; margin:0; padding:17px 18px;
                      background:#f4f7fb; border:1px solid #d8e1ee; border-left:7px solid #5f7ea8;
                      border-radius:10px; box-shadow:0 5px 14px #2139570c; }
                    .executive-cards li:hover,.final-recommendation-cards li:hover { transform:translateY(-1px);
                      box-shadow:0 8px 18px #21395716; }
                    .executive-cards .card-application { background:var(--blue-soft); border-color:#b7d2ef; border-left-color:#3478c9; }
                    .executive-cards .card-decision { background:var(--orange-soft); border-color:#edbd97; border-left-color:#e56b16; }
                    .executive-cards .card-flaws { background:var(--red-soft); border-color:#e3acb5; border-left-color:#b42336; }
                    .executive-cards .card-risk { background:#fff4e8; border-color:#efc4a0; border-left-color:#ef7d20; }
                    .executive-cards .card-developer { background:var(--yellow-soft); border-color:#e1ce7c; border-left-color:#d5a400; }
                    .executive-cards .card-architect { background:var(--purple-soft); border-color:#cab8e7; border-left-color:#7451b9; }
                    .final-recommendation-cards .card-developer { background:var(--yellow-soft); border-color:#e1ce7c;
                      border-left-color:#d5a400; }
                    .final-recommendation-cards .card-architect { background:var(--purple-soft); border-color:#cab8e7;
                      border-left-color:#7451b9; }
                    .final-recommendation-cards .card-exit { background:var(--green-soft); border-color:#acd8bc;
                      border-left-color:#25864b; }
                    .final-recommendation-cards .card-rerun { background:var(--blue-soft); border-color:#b7d2ef;
                      border-left-color:#3478c9; }
                    table { width:100%; margin:17px 0 28px; table-layout:fixed; border-collapse:separate;
                      border-spacing:0; overflow:hidden; border:1px solid var(--line-strong); border-radius:10px;
                      box-shadow:0 5px 16px #243b5a0d; }
                    thead { background:linear-gradient(180deg,#eaf1fb,#dfe9f7); }
                    th { padding:11px 12px; color:#17345f; text-align:left; font-size:.83rem;
                      letter-spacing:.025em; text-transform:uppercase; }
                    th,td { border:0; border-right:1px solid var(--line); border-bottom:1px solid var(--line);
                      vertical-align:top; overflow-wrap:anywhere; }
                    th:last-child,td:last-child { border-right:0; }
                    tbody tr:last-child td { border-bottom:0; }
                    td { padding:10px 12px; background:#fff; }
                    tbody tr:nth-child(even) td { background:#f9fbfd; }
                    tbody tr:hover td { filter:brightness(.985); }
                    tr.finding-blocker td { background:#fff0f2; border-color:#e4a8b1; }
                    tr.finding-high td { background:#fff5ed; border-color:#efc5a5; }
                    tr.finding-medium td { background:#fffbea; border-color:#e8d68f; }
                    tr.finding-low td { background:#eef6ff; border-color:#accbed; }
                    tr.finding-blocker td:first-child { box-shadow:inset 5px 0 #b42336; }
                    tr.finding-high td:first-child { box-shadow:inset 5px 0 #e56b16; }
                    tr.finding-medium td:first-child { box-shadow:inset 5px 0 #d5a400; }
                    tr.finding-low td:first-child { box-shadow:inset 5px 0 #3478c9; }
                    tr.finding-blocker td:nth-child(3) { color:#82162a; background:#ffdce2; font-weight:750; }
                    tr.finding-high td:nth-child(3) { color:#93400b; background:#ffe4d0; font-weight:750; }
                    tr.finding-medium td:nth-child(3) { color:#785b00; background:#fff1b5; font-weight:750; }
                    tr.finding-low td:nth-child(3) { color:#164e8d; background:#dcecff; font-weight:750; }
                    tr.finding-blocker td:first-child a,tr.finding-high td:first-child a,
                    tr.finding-medium td:first-child a,tr.finding-low td:first-child a { display:inline-grid;
                      width:2rem; height:2rem; place-items:center; color:#fff; text-decoration:none; border-radius:999px; }
                    tr.finding-blocker td:first-child a { background:#b42336; }
                    tr.finding-high td:first-child a { background:#d85d0d; }
                    tr.finding-medium td:first-child a { background:#b88b00; }
                    tr.finding-low td:first-child a { background:#3478c9; }
                    tr.status-pass td { background:#edf9f1; border-color:#b9ddc6; }
                    tr.status-fail td { background:#fff0f2; border-color:#e6b3bc; }
                    tr.status-unverified td { background:#f2f4f7; border-color:#c9d0da; }
                    tr.status-pass td:first-child { box-shadow:inset 5px 0 #25864b; }
                    tr.status-fail td:first-child { box-shadow:inset 5px 0 #b42336; }
                    tr.status-unverified td:first-child { box-shadow:inset 5px 0 #6b7f9e; }
                    tr.status-pass td:nth-child(3) { color:#176237; background:#d9f1e2; font-weight:800; }
                    tr.status-fail td:nth-child(3) { color:#82162a; background:#ffdee4; font-weight:800; }
                    tr.status-unverified td:nth-child(3) { color:#344967; background:#e3e8ef; font-weight:800; }
                    tr.readiness-developer td { background:#fff8e8; border-color:#ead49a; }
                    tr.readiness-developer td:first-child { box-shadow:inset 5px 0 #e56b16; }
                    tr.readiness-architect td { background:var(--purple-soft); border-color:#d3c5e9; }
                    tr.readiness-architect td:first-child { box-shadow:inset 5px 0 #7451b9; }
                    tr.readiness-production td { background:var(--red-soft); border-color:#e5b4bc; }
                    tr.readiness-production td:first-child { box-shadow:inset 5px 0 #b42336; }
                    tr.verification-pass td { background:var(--green-soft); border-color:#b8ddc5; }
                    tr.verification-pass td:first-child { box-shadow:inset 5px 0 #25864b; }
                    @media (max-width:900px) { main { margin:0; padding:28px 20px 42px; border:0; border-radius:0; }
                      .executive-cards,.final-recommendation-cards { grid-template-columns:1fr; }
                      table { display:block; overflow-x:auto; } }
                    @media print { body { background:white; font-size:10pt; }
                      main { margin:0; padding:0; border:0; box-shadow:none; max-width:none; }
                      h2,h3,blockquote,table { break-inside:avoid; }
                      .executive-cards li,.final-recommendation-cards li { box-shadow:none; } }
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
        int codeIndent = 0;
        String currentSection = "";

        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            var strippedLine = line.stripLeading();
            if (strippedLine.startsWith("```")) {
                if (inList) { out.append("</ul>\n"); inList = false; }
                if (inCode) {
                    out.append("</code></pre>\n");
                } else {
                    codeIndent = line.length() - strippedLine.length();
                    var language = strippedLine.substring(3).strip();
                    out.append(language.equals("diff") ? "<pre class=\"code-diff\"><code>" : "<pre><code>");
                }
                inCode = !inCode;
                continue;
            }
            if (inCode) {
                var codeLine = line;
                if (codeIndent > 0 && line.length() >= codeIndent
                        && line.substring(0, codeIndent).isBlank()) {
                    codeLine = line.substring(codeIndent);
                }
                out.append(escape(codeLine)).append('\n');
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
                    out.append("<tr class=\"").append(tableRowClass(rows.get(row))).append("\">");
                    rows.get(row).forEach(cell -> out.append("<td>").append(inline(cell)).append("</td>"));
                    out.append("</tr>\n");
                }
                out.append("</tbody></table>\n");
                continue;
            }
            if (line.startsWith(">")) {
                var content = line.substring(1).stripLeading();
                if (!inQuote) {
                    out.append("<blockquote class=\"").append(decisionClass(content)).append("\">\n");
                    inQuote = true;
                }
                if (content.startsWith("## ")) out.append("<h2>").append(inline(content.substring(3))).append("</h2>\n");
                else if (!content.isBlank()) out.append("<p>").append(inline(content)).append("</p>\n");
                continue;
            } else if (inQuote) {
                out.append("</blockquote>\n");
                inQuote = false;
            }
            if (line.startsWith("- ")) {
                if (!inList) {
                    out.append("<ul class=\"").append(listClass(currentSection)).append("\">\n");
                    inList = true;
                }
                var item = line.substring(2);
                out.append("<li class=\"").append(listItemClass(currentSection, item)).append("\">")
                        .append(inline(item)).append("</li>\n");
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
                var anchor = issueAnchor(heading);
                out.append("<h3");
                if (!anchor.isEmpty()) out.append(" id=\"").append(anchor).append("\"");
                out.append(" class=\"").append(severityClass).append("\">")
                        .append(inline(heading)).append("</h3>\n");
            }
            else if (line.startsWith("## ")) {
                currentSection = line.substring(3);
                out.append("<h2>").append(inline(currentSection)).append("</h2>\n");
            }
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

    private static String decisionClass(String content) {
        if (content.contains("ENGINEERING READINESS")) return "decision-readiness";
        if (content.contains("🟢")) return "decision-pass";
        if (content.contains("🟡")) return "decision-followup";
        if (content.contains("🟠")) return "decision-changes";
        if (content.contains("🔴")) return "decision-blocked";
        if (content.contains("⚪")) return "decision-evidence";
        return "";
    }

    private static String listClass(String section) {
        if (section.equals("Executive assessment")
                || section.equals("Overall engineering assessment")) return "executive-cards";
        if (section.equals("Final recommendation")) return "final-recommendation-cards";
        return "";
    }

    private static String listItemClass(String section, String item) {
        if (section.equals("Overall engineering assessment")) {
            if (item.startsWith("**Application:**")) return "card-application";
            if (item.startsWith("**Readiness:**")) return "card-decision";
            if (item.startsWith("**Verified findings:**")) return "card-flaws";
            if (item.startsWith("**Top risk:**")) return "card-risk";
            if (item.startsWith("**Developer action:**")) return "card-developer";
            if (item.startsWith("**Architect action:**")) return "card-architect";
        }
        if (section.equals("Final recommendation")) {
            if (item.startsWith("**Developer next step:**")
                    || item.startsWith("**🟡 Developer next step:**")) return "card-developer";
            if (item.startsWith("**Architect next step:**")
                    || item.startsWith("**🟣 Architect next step:**")) return "card-architect";
            if (item.startsWith("**Release condition:**")
                    || item.startsWith("**🟢 Exit criteria:**")) return "card-exit";
            if (item.startsWith("**Re-review:**")
                    || item.startsWith("**🔵 Re-review:**")) return "card-rerun";
        }
        return "";
    }

    private static String tableRowClass(List<String> cells) {
        var text = String.join(" ", cells);
        if (text.contains("🔴")) return "finding-blocker";
        if (text.contains("🟠")) return "finding-high";
        if (text.contains("🟡")) return "finding-medium";
        if (text.contains("🔵")) return "finding-low";
        if (text.contains("✅ Meets baseline")) return "status-pass";
        if (text.contains("❌ Below baseline")) return "status-fail";
        if (text.contains("❓ Insufficient evidence")) return "status-unverified";
        if (text.startsWith("Developer implementation readiness ")) return "readiness-developer";
        if (text.startsWith("Architect review readiness ")) return "readiness-architect";
        if (text.startsWith("Production readiness ")) return "readiness-production";
        if (text.startsWith("Build and tests PASS")
                || text.startsWith("Test coverage ") && text.contains("gate passed")) {
            return "verification-pass";
        }
        return "";
    }

    private static String issueAnchor(String heading) {
        var matcher = ISSUE_HEADING.matcher(heading);
        return matcher.find() ? "issue-" + matcher.group(1) : "";
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
