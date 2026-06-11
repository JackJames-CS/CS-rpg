import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** All game state + derived calculations + persistence. */
class Player {
    String name = "Dev";
    int level = 1, xp = 0;
    int money = 0;
    int stamina = 100, staminaMax = 100, burnout = 0;
    int skillPoints = 0, portfolio = 0;
    int laptopTier = 0;        // 0 hairdryer, 1 thinkpad, 2 own machine, 3 dream machine
    boolean loaner = false;    // company loaner laptop (Ch2)
    int rideTier = 0;          // 0 shoes, 1 bike, 2 starter car, 3 reliable car
    String careerPath = "";    // senior choose: backend/frontend/ai/devops
    String job = "";
    int prestige = 0;
    boolean tutorialDone = false;
    String title = "";
    boolean graduated = false;
    int imposter = 0;          // active from Ch3 (init by story)
    int reputation = 0;
    int culture = 50;          // intern phase
    int internWork = 0;        // work actions during intern phase (review score)

    Map<String, Integer> inventory = new LinkedHashMap<>();
    Map<String, Integer> skills = new LinkedHashMap<>();
    Set<String> achieved = new LinkedHashSet<>();
    List<Data.Quest> quests = Data.defaultQuests();

    // story state
    Map<String, String> flags = new LinkedHashMap<>();
    Map<String, Integer> rel = new LinkedHashMap<>();
    Set<String> beatsSeen = new LinkedHashSet<>();

    // courses: attends in progress, grades for examined courses (0 fail .. 3 distinction)
    Map<String, Integer> courseAttends = new LinkedHashMap<>();
    Map<String, Integer> courseGrades = new LinkedHashMap<>();

    // ---------------------------------------------------------------- derived

    String phase() {
        if (level <= 16) return "STUDENT";
        if (level <= 24) return "INTERN";
        if (level <= 40) return "JUNIOR";
        return "SENIOR";
    }

    String phaseLabel() { return phaseLabelAt(level); }

    String phaseLabelAt(int lv) {
        if (lv <= 16) return "Student (Year " + Math.min(4, (lv - 1) / 4 + 1) + ")";
        if (lv <= 24) return "Intern";
        if (lv <= 40) return "Junior Developer";
        return flags.getOrDefault("seniorTrack", "ic").equals("mgmt") ? "Team Lead" : "Staff Engineer";
    }

    int year() { return Math.min(4, (level - 1) / 4 + 1); }

    int xpToNext() {
        if (level <= 16) return 60 + level * 5;
        if (level <= 24) return 90 + (level - 16) * 10;
        if (level <= 40) return 130 + (level - 24) * 8;
        return 170 + (level - 40) * 10;
    }

    /** Burnout/stamina penalty on all money+XP gains. */
    double gainMult() {
        double m = 1.0;
        if (burnout >= 75) m *= 0.70;
        else if (burnout >= 50) m *= 0.85;
        if (stamina <= 20) m *= 0.85;
        if (imposter >= 85) m *= 0.90;
        return m;
    }

    boolean has(String itemKey) { return inventory.getOrDefault(itemKey, 0) > 0; }
    int skill(String s) { return skills.getOrDefault(s, 0); }

    String flag(String k) { return flags.getOrDefault(k, ""); }
    boolean is(String k, String v) { return flag(k).equals(v); }
    boolean isTrue(String k) { return flag(k).equals("true"); }
    void set(String k, String v) { flags.put(k, v); }

    int relOf(String npc) { return rel.getOrDefault(npc, 0); }
    void relAdd(String npc, int d) { rel.merge(npc, d, Integer::sum); }

    boolean seen(String beatId) { return beatsSeen.contains(beatId); }

    int flagInt(String k) {
        try { return Integer.parseInt(flag(k)); } catch (NumberFormatException e) { return 0; }
    }
    void flagAdd(String k, int d) { set(k, String.valueOf(flagInt(k) + d)); }

    // ---------------------------------------------------------------- courses

    boolean coursePassed(String key) { return courseGrades.getOrDefault(key, -1) >= 1; }

    boolean coreCoursesPassed() {
        for (String c : Data.CORE_COURSES) if (!coursePassed(c)) return false;
        return true;
    }

    /** GPA across all examined courses (fails count 0). -1 if none taken. */
    double gpa() {
        if (courseGrades.isEmpty()) return -1;
        double sum = 0;
        for (int g : courseGrades.values()) sum += g;
        return sum / courseGrades.size();
    }

    String gpaTier() {
        double g = gpa();
        if (g >= 2.5) return "high";
        if (g >= 1.5) return "mid";
        return "low";
    }

    // ---------------------------------------------------------------- mutation

    void addMoney(int d) {
        money += d;
        if (money < 0) money = 0;
    }

    void addStamina(int d) {
        stamina = Math.max(0, Math.min(staminaMax, stamina + d));
    }

    /** Imposter meter only moves once Ch3 has initialised it (level 25+). */
    void imposterBump(int d) { if (level >= 25) imposter = Math.min(100, imposter + d); }
    void imposterShip(int d) { if (level >= 25) imposter = Math.max(0, imposter - d); }

    /** Apply burnout gain with Scar Tissue perks; negative d = recovery (unscaled). */
    void addBurnout(int d) {
        if (d > 0) {
            double m = 1.0;
            if (isTrue("scarTissue1")) m *= 0.9;
            if (isTrue("scarTissue2")) m *= 0.9;
            d = (int) Math.max(1, Math.round(d * m));
        }
        burnout = Math.max(0, Math.min(100, burnout + d));
    }

    /**
     * Add XP, handling level-ups, the level-16 graduation cap and the level-50 cap.
     * Returns list of levels reached (story beats fire per level, in order).
     */
    List<Integer> addXp(int d) {
        List<Integer> ups = new ArrayList<>();
        xp += d;
        while (true) {
            int need = xpToNext();
            if (xp < need) break;
            if (level == 16 && !graduated) { xp = need; break; }   // held at the cap until graduation
            if (level >= 50) { xp = need; break; }
            xp -= need;
            level++;
            burnout = Math.max(0, burnout - 10);
            ups.add(level);
        }
        return ups;
    }

    // ---------------------------------------------------------------- persistence

    void save(Path file) throws IOException {
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
            w.println("name=" + name);
            w.println("level=" + level);
            w.println("xp=" + xp);
            w.println("money=" + money);
            w.println("stamina=" + stamina);
            w.println("staminaMax=" + staminaMax);
            w.println("burnout=" + burnout);
            w.println("skillPoints=" + skillPoints);
            w.println("portfolio=" + portfolio);
            w.println("laptopTier=" + laptopTier);
            w.println("loaner=" + loaner);
            w.println("rideTier=" + rideTier);
            w.println("careerPath=" + careerPath);
            w.println("job=" + job);
            w.println("prestige=" + prestige);
            w.println("tutorialDone=" + tutorialDone);
            w.println("title=" + title);
            w.println("graduated=" + graduated);
            w.println("imposter=" + imposter);
            w.println("reputation=" + reputation);
            w.println("culture=" + culture);
            w.println("internWork=" + internWork);
            for (Map.Entry<String, Integer> e : inventory.entrySet()) w.println("I|" + e.getKey() + "=" + e.getValue());
            for (Map.Entry<String, Integer> e : skills.entrySet())    w.println("S|" + e.getKey() + "=" + e.getValue());
            for (String a : achieved)                                 w.println("A|" + a);
            for (Data.Quest q : quests)                               w.println("Q|" + q.id + "=" + q.progress + "," + q.done);
            for (Map.Entry<String, String> e : flags.entrySet())      w.println("F|" + e.getKey() + "=" + e.getValue());
            for (Map.Entry<String, Integer> e : rel.entrySet())       w.println("R|" + e.getKey() + "=" + e.getValue());
            for (String b : beatsSeen)                                w.println("B|" + b);
            for (Map.Entry<String, Integer> e : courseGrades.entrySet())  w.println("G|" + e.getKey() + "=" + e.getValue());
            for (Map.Entry<String, Integer> e : courseAttends.entrySet()) w.println("C|" + e.getKey() + "=" + e.getValue());
        }
    }

    static Player load(Path file) throws IOException {
        Player p = new Player();
        // default quests already present; path quests may be appended below
        Map<String, Data.Quest> byId = new LinkedHashMap<>();
        for (Data.Quest q : p.quests) byId.put(q.id, q);

        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            if (line.startsWith("I|")) { String[] kv = split(line); p.inventory.put(kv[0], Integer.parseInt(kv[1])); }
            else if (line.startsWith("S|")) { String[] kv = split(line); p.skills.put(kv[0], Integer.parseInt(kv[1])); }
            else if (line.startsWith("A|")) { p.achieved.add(line.substring(2)); }
            else if (line.startsWith("Q|")) {
                String[] kv = split(line);
                String[] pd = kv[1].split(",");
                Data.Quest q = byId.get(kv[0]);
                if (q == null) { // path quest not in defaults
                    for (String path : new String[]{"backend", "frontend", "ai", "devops"})
                        for (Data.Quest pq : Data.pathQuests(path))
                            if (pq.id.equals(kv[0])) { q = pq; p.quests.add(q); byId.put(q.id, q); }
                }
                if (q != null) { q.progress = Integer.parseInt(pd[0]); q.done = Boolean.parseBoolean(pd[1]); }
            }
            else if (line.startsWith("F|")) { String[] kv = split(line); p.flags.put(kv[0], kv[1]); }
            else if (line.startsWith("R|")) { String[] kv = split(line); p.rel.put(kv[0], Integer.parseInt(kv[1])); }
            else if (line.startsWith("B|")) { p.beatsSeen.add(line.substring(2)); }
            else if (line.startsWith("G|")) { String[] kv = split(line); p.courseGrades.put(kv[0], Integer.parseInt(kv[1])); }
            else if (line.startsWith("C|")) { String[] kv = split(line); p.courseAttends.put(kv[0], Integer.parseInt(kv[1])); }
            else {
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String k = line.substring(0, eq), v = line.substring(eq + 1);
                switch (k) {
                    case "name": p.name = v; break;
                    case "level": p.level = Integer.parseInt(v); break;
                    case "xp": p.xp = Integer.parseInt(v); break;
                    case "money": p.money = Integer.parseInt(v); break;
                    case "stamina": p.stamina = Integer.parseInt(v); break;
                    case "staminaMax": p.staminaMax = Integer.parseInt(v); break;
                    case "burnout": p.burnout = Integer.parseInt(v); break;
                    case "skillPoints": p.skillPoints = Integer.parseInt(v); break;
                    case "portfolio": p.portfolio = Integer.parseInt(v); break;
                    case "laptopTier": p.laptopTier = Integer.parseInt(v); break;
                    case "loaner": p.loaner = Boolean.parseBoolean(v); break;
                    case "rideTier": p.rideTier = Integer.parseInt(v); break;
                    case "careerPath": p.careerPath = v; break;
                    case "job": p.job = v; break;
                    case "prestige": p.prestige = Integer.parseInt(v); break;
                    case "tutorialDone": p.tutorialDone = Boolean.parseBoolean(v); break;
                    case "title": p.title = v; break;
                    case "graduated": p.graduated = Boolean.parseBoolean(v); break;
                    case "imposter": p.imposter = Integer.parseInt(v); break;
                    case "reputation": p.reputation = Integer.parseInt(v); break;
                    case "culture": p.culture = Integer.parseInt(v); break;
                    case "internWork": p.internWork = Integer.parseInt(v); break;
                }
            }
        }
        return p;
    }

    private static String[] split(String line) {
        String body = line.substring(2);
        int eq = body.indexOf('=');
        return new String[]{ body.substring(0, eq), body.substring(eq + 1) };
    }
}
