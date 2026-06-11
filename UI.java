import java.util.Scanner;

/** Terminal helpers: colours, cards, scenes, choice prompts. One Scanner for the whole game. */
class UI {
    static final Scanner IN = new Scanner(System.in);
    /** False when input is piped (tests) — skips pauses and typing delays. */
    static final boolean INTERACTIVE = detectInteractive();

    private static boolean detectInteractive() {
        java.io.Console c = System.console();
        if (c == null) return false;
        try { // JDK 22+: console() is non-null even when piped; isTerminal() tells the truth
            return (Boolean) java.io.Console.class.getMethod("isTerminal").invoke(c);
        } catch (Exception e) {
            return true; // pre-22: non-null console means a real terminal
        }
    }
    static boolean color = true;

    static String c(String code, String s) { return color ? "[" + code + "m" + s + "[0m" : s; }

    static void say(String s)  { System.out.println(s); }
    static void ok(String s)   { System.out.println(c("32", s)); }
    static void info(String s) { System.out.println(c("36", s)); }
    static void warn(String s) { System.out.println(c("33", s)); }
    static void err(String s)  { System.out.println(c("31", s)); }
    static void dim(String s)  { System.out.println(c("90", s)); }
    static void gold(String s) { System.out.println(c("33;1", s)); }
    static void blank()        { System.out.println(); }
    static void hr()           { dim("  ------------------------------------------------------------"); }

    /** Chapter/year card: bordered block, waits for enter when interactive. */
    static void card(String title, String... lines) {
        blank();
        gold("  ============================================================");
        if (title != null && !title.isEmpty()) {
            gold("    " + title);
            gold("  ============================================================");
        }
        for (String l : lines) say("    " + l);
        gold("  ============================================================");
        pause();
    }

    /** Story scene: indented lines with a slight beat between them. */
    static void scene(String... lines) {
        blank();
        for (String l : lines) {
            say("  " + l);
            sleep(110);
        }
        blank();
    }

    static void narrate(String s) { info("  " + s); sleep(110); }

    static void pause() {
        if (!INTERACTIVE) return;
        dim("  [enter]");
        IN.nextLine();
    }

    /** Numbered choice, returns 0-based index. */
    static int choose(String prompt, String... options) {
        if (prompt != null && !prompt.isEmpty()) { blank(); warn("  " + prompt); }
        for (int i = 0; i < options.length; i++) say("    " + (i + 1) + ") " + options[i]);
        while (true) {
            System.out.print("  > ");
            String s = IN.nextLine().trim();
            try {
                int n = Integer.parseInt(s);
                if (n >= 1 && n <= options.length) return n - 1;
            } catch (NumberFormatException ignored) {}
            dim("  pick 1-" + options.length);
        }
    }

    static String readLine(String prompt) {
        System.out.print(prompt);
        return IN.nextLine().trim();
    }

    static void sleep(int ms) {
        if (!INTERACTIVE) return;
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
