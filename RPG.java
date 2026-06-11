import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** CS Career RPG — main loop and commands. */
public class RPG {
    static Player p;
    static final Random RNG = new Random();
    static final Path DEFAULT_SAVE = Path.of("savegame.txt");
    static boolean running = true;

    public static void main(String[] args) {
        UI.gold("  ============================================");
        UI.gold("       CS CAREER RPG -- the long version");
        UI.gold("  ============================================");
        UI.blank();

        if (Files.exists(DEFAULT_SAVE)) {
            int c = UI.choose("A save exists.", "Continue", "New game (overwrites on next save)");
            if (c == 0) {
                try {
                    p = Player.load(DEFAULT_SAVE);
                    Story.p = p;
                    UI.ok("  Welcome back, " + p.name + ". Lv " + p.level + ", EUR " + p.money + ".");
                } catch (IOException e) {
                    UI.err("  Save unreadable. Starting fresh.");
                }
            }
        }
        if (p == null) newGame();

        UI.dim("  Type `help` for commands. `tutorial` if it's your first run.");
        while (running) {
            UI.blank();
            String line = UI.readLine("[Lv " + p.level + " | EUR " + p.money + "] > ");
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+", 2);
            String cmd = parts[0].toLowerCase();
            String arg = parts.length > 1 ? parts[1].trim() : "";
            dispatch(cmd, arg);
            autosave();
        }
    }

    static void newGame() {
        p = new Player();
        Story.p = p;
        String n = UI.readLine("  Your name: ");
        if (!n.isEmpty()) p.name = n;
        Story.prologue();
    }

    // ---------------------------------------------------------------- dispatch

    static void dispatch(String cmd, String arg) {
        switch (cmd) {
            case "help": case "h": help(); break;
            case "status": case "st": status(); break;
            case "inventory": case "i": inventory(); break;
            case "jobs": case "j": jobs(); break;
            case "setjob": case "job": setjob(arg); break;
            case "work": case "w": work(); break;
            case "study": case "s": study(); break;
            case "learn": learn(); break;
            case "project": case "p": project(); break;
            case "team": case "t": team(); break;
            case "rest": case "r": rest(); break;
            case "attend": attend(arg); break;
            case "courses": courses(); break;
            case "gpa": gpa(); break;
            case "sideproject": case "startupwork": sideproject(); break;
            case "shop": shop(); break;
            case "buy": case "b": buy(arg); break;
            case "use": case "u": use(arg); break;
            case "sell": sell(arg); break;
            case "inspect": inspect(arg); break;
            case "deals": deals(); break;
            case "upgrade": upgrade(arg); break;
            case "skills": skillsCmd(); break;
            case "learnskill": learnSkill(arg); break;
            case "path": pathCmd(); break;
            case "choose": choosePath(arg); break;
            case "quests": case "q": questsCmd(); break;
            case "achievements": case "ach": achievementsCmd(); break;
            case "tutorial": tutorial(); break;
            case "prestige": prestigeCmd(); break;
            case "doors": doorsCmd(); break;
            case "event": Events.fire(p, arg.isEmpty() ? null : arg); afterAction(false); break;
            case "dev": devCmd(arg); break;
            case "save": saveCmd(arg); break;
            case "load": loadCmd(arg); break;
            case "saves": savesCmd(); break;
            case "export": exportCmd(arg); break;
            case "quit": case "exit": quit(); break;
            default: UI.dim("  Unknown command. `help` lists everything.");
        }
    }

    // ---------------------------------------------------------------- xp / actions plumbing

    /** All XP flows through here so story beats fire on every level-up. */
    static void gainXp(int amount) {
        List<Integer> ups = p.addXp(amount);
        for (int lv : ups) {
            UI.gold("  *** LEVEL UP -> " + lv + " (" + p.phaseLabelAt(lv) + ") ***");
            Story.onLevelUp(lv);
        }
        if (p.level == 16 && !p.graduated && p.xp >= p.xpToNext())
            UI.dim("  You can't intern your way out of a degree. Finish the thing. (core: CS101, Data Structures, Algorithms)");
    }

    static int burnoutPerAction(boolean isWork) {
        int b = p.has("standingdesk") ? 3 : 6;
        if (isWork) b = Math.max(0, b - Data.RIDE_BURN[p.rideTier]);
        return b;
    }

    static void afterAction(boolean event) {
        questPortfolioCheck();
        checkAchievements();
        Story.checkGates();
        Story.hook("fraud");
        if (event) Events.maybeFire(p);
    }

    static boolean tooTired() {
        if (p.stamina < 10) {
            UI.warn("  You're running on empty. `rest`, or coffee if you must.");
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- core actions

    static Data.Job currentJob() {
        Data.Job best = null;
        for (Data.Job j : Data.JOBS) {
            boolean phaseOk = Data.phaseRank(j.phase) <= Data.phaseRank(p.phase());
            boolean laptopOk = !j.needsLaptop || p.laptopTier >= 1 || p.loaner;
            if (!phaseOk || !laptopOk) continue;
            if (j.name.equalsIgnoreCase(p.job)) return j;
            if (best == null || j.pay > best.pay) best = j;
        }
        return best;
    }

    static int earnings(Data.Job j) {
        int bonus = 0;
        if (p.has("keyboard")) bonus += 15;
        bonus += Data.LAPTOP_BONUS[p.laptopTier];
        if (p.loaner) bonus += 15;
        bonus += p.portfolio / 25;
        bonus += p.skill(j.tag) * 2;
        String path = p.careerPath.equals("ai") ? "ml" : p.careerPath;
        if (p.level >= 41 && !p.careerPath.isEmpty() && j.tag.equals(path)) bonus += 40;
        double total = (j.pay + bonus) * (1 + p.prestige * 0.05) * p.gainMult();
        if (p.isTrue("founderScar")) total *= 1.10;
        return (int) Math.round(total);
    }

    static void work() {
        if (p.burnout >= 80) {
            Story.hook("wall");
            UI.err("  Burned out. Work is locked until you recover. `rest`.");
            return;
        }
        if (tooTired()) return;
        Data.Job j = currentJob();
        if (j == null) {
            UI.warn("  No job you can do yet. Coding jobs need a real laptop -- `upgrade laptop`.");
            return;
        }
        if (!j.name.equalsIgnoreCase(p.job)) { p.job = j.name; UI.dim("  (Working as: " + j.name + ")"); }
        int pay = earnings(j);
        int xp = (int) Math.round(j.xp * p.gainMult());
        p.addMoney(pay);
        p.addStamina(-15);
        p.addBurnout(burnoutPerAction(true));
        if (p.phase().equals("INTERN")) {
            p.internWork++;
            if (p.stamina < 30) { p.culture = Math.max(0, p.culture - 2); }
        }
        p.imposterShip(1);
        UI.ok("  " + j.name + ": EUR +" + pay + ", +" + xp + " XP");
        gainXp(xp);
        questProgress("work");
        afterAction(true);
    }

    static void study() {
        if (tooTired()) return;
        double xp = 12 + p.level / 4.0;
        if (p.coursePassed("algorithms")) xp *= 1.25;
        if (p.isTrue("studyBonus")) xp *= 1.10;
        if (p.has("headphones")) xp *= 1.25;
        int gained = (int) Math.round(xp * p.gainMult());
        p.addStamina(-10);
        p.addBurnout(burnoutPerAction(false));
        UI.ok("  Study: +" + gained + " XP");
        gainXp(gained);
        questProgress("study");
        afterAction(true);
    }

    static void learn() {
        if (p.level < 17) { UI.warn("  `learn` unlocks at the internship (level 17)."); return; }
        if (tooTired()) return;
        p.skillPoints += 2;
        p.addStamina(-10);
        p.addBurnout(burnoutPerAction(false));
        UI.ok("  Learn: +2 skill points (" + p.skillPoints + " total)");
        gainXp((int) Math.round(8 * p.gainMult()));
        questProgress("learn");
        afterAction(true);
    }

    static void project() {
        if (tooTired()) return;
        int gain = 2 + (p.has("tower") ? 1 : 0);
        if (p.has("dualmonitors")) gain = (int) Math.round(gain * 1.5);
        p.portfolio += gain;
        p.addStamina(-15);
        p.addBurnout(burnoutPerAction(false));
        p.imposterShip(2);
        UI.ok("  Project: +" + gain + " portfolio (" + p.portfolio + ")");
        gainXp((int) Math.round(10 * p.gainMult()));
        questProgress("project");
        afterAction(true);
    }

    static void team() {
        if (p.level < 25) { UI.warn("  Team collaboration unlocks at junior (level 25)."); return; }
        if (tooTired()) return;
        int pay = (int) Math.round((30 + p.level) * (1 + p.prestige * 0.05) * p.gainMult());
        p.addMoney(pay);
        p.portfolio += 1;
        p.addStamina(-18);
        p.addBurnout(burnoutPerAction(false));
        UI.ok("  Team: EUR +" + pay + ", +1 portfolio");
        gainXp((int) Math.round(12 * p.gainMult()));
        questProgress("team");
        afterAction(true);
    }

    static void rest() {
        int recover = 35 + (p.has("deskplant") ? 5 : 0);
        p.addStamina(recover);
        p.addBurnout(-22);
        UI.ok("  Rest: +" + recover + " stamina, -22 burnout  (stamina " + p.stamina + ", burnout " + p.burnout + ")");
        afterAction(false);
    }

    static void sideproject() {
        if (p.level < 25) { UI.warn("  Side projects unlock at junior (level 25)."); return; }
        if (tooTired()) return;
        boolean selkie = p.isTrue("joinedSelkie") && p.flag("selkieOutcome").isEmpty();
        int mult = p.laptopTier >= 2 ? 2 : 1; // Your Own Machine: x1.5, rounded up
        if (selkie) {
            p.addStamina(-24);
            p.flagAdd("selkieScore", mult);
            p.portfolio += 1;
            UI.ok("  Selkie work: nights and weekends. +1 portfolio, the equity vests in hope.");
            UI.dim("  (Ciaran pushed " + (3 + RNG.nextInt(9)) + " commits at 3am. All named 'fix'.)");
        } else {
            p.addStamina(-12);
            p.portfolio += mult;
            p.reputation += 1;
            UI.ok("  Side project: +" + mult + " portfolio, +1 reputation");
        }
        p.addBurnout(burnoutPerAction(false));
        gainXp((int) Math.round(8 * p.gainMult()));
        questProgress("project");
        afterAction(true);
    }

    // ---------------------------------------------------------------- courses (§3)

    static Data.Course findCourse(String arg) {
        String a = arg.toLowerCase().replace(" ", "");
        for (Data.Course c : Data.COURSES.values())
            if (c.key.equals(a) || c.name.toLowerCase().replace(" ", "").contains(a)) return c;
        return null;
    }

    static void attend(String arg) {
        if (p.level > 16) { UI.warn("  College is over. The rain isn't."); return; }
        if (arg.isEmpty()) { courses(); return; }
        Data.Course c = findCourse(arg);
        if (c == null) { UI.warn("  No such course. `courses` lists them."); return; }
        if (p.coursePassed(c.key)) { UI.dim("  Already passed. Brennan would call re-attending 'theatre'."); return; }
        if (c.year > p.year()) { UI.warn("  " + c.name + " is a Year " + c.year + " module. You're in Year " + p.year() + "."); return; }
        for (String pre : c.prereqs)
            if (!p.coursePassed(pre)) { UI.warn("  Prerequisite missing: " + Data.COURSES.get(pre).name); return; }
        if (tooTired()) return;

        p.addStamina(-12);
        p.addBurnout(burnoutPerAction(false));
        int attends = p.courseAttends.merge(c.key, 1, Integer::sum);
        double xp = 15;
        if (p.has("headphones")) xp *= 1.25;
        gainXp((int) Math.round(xp * p.gainMult()));
        if (attends < 3) {
            UI.ok("  " + c.name + ": lecture " + attends + "/3 attended.");
        } else {
            exam(c);
        }
        afterAction(true);
    }

    static void exam(Data.Course c) {
        UI.blank();
        UI.warn("  *** EXAM: " + c.name + " ***  (3 questions)");
        int right = 0;
        for (Data.Question q : c.bank) if (Events.ask(q)) right++;
        String[] names = { "FAIL", "Pass", "Merit", "DISTINCTION" };
        UI.blank();
        if (right == 0) {
            UI.err("  0/3 -- FAIL. One more attend and you can resit.");
            p.courseAttends.put(c.key, 2);
            p.courseGrades.put(c.key, 0);
            return;
        }
        p.courseGrades.put(c.key, right);
        p.courseAttends.remove(c.key);
        UI.gold("  " + right + "/3 -- " + names[right] + " in " + c.name);
        int skillGain = right == 3 ? 2 : right == 2 ? 1 : 0;
        if (skillGain > 0) {
            p.skills.merge(c.skill, skillGain, Integer::sum);
            UI.info("  +" + skillGain + " " + c.skill + " skill");
        }
        if (right == 3) Story.hook("distinction");
        if (c.key.equals("algorithms")) UI.dim("  (Algorithms passed: study XP x1.25 from now on.)");
        Story.checkGates();
    }

    static void courses() {
        UI.info("  Year " + p.year() + ". Modules (attend <course> -- exam on the 3rd lecture):");
        for (Data.Course c : Data.COURSES.values()) {
            String status;
            int grade = p.courseGrades.getOrDefault(c.key, -1);
            if (grade >= 1) status = new String[]{"", "Pass", "Merit", "DISTINCTION"}[grade];
            else if (grade == 0) status = "FAILED -- resit available";
            else if (c.year > p.year()) status = "Year " + c.year;
            else {
                boolean ok = true;
                StringBuilder need = new StringBuilder();
                for (String pre : c.prereqs)
                    if (!p.coursePassed(pre)) { ok = false; need.append(Data.COURSES.get(pre).name); }
                status = ok ? (p.courseAttends.getOrDefault(c.key, 0) + "/3 attended") : "needs " + need;
            }
            UI.say("    " + pad(c.key, 15) + pad(c.name, 30) + status);
        }
        UI.dim("  Core for graduation: CS101, Data Structures, Algorithms.");
    }

    static void gpa() {
        if (p.courseGrades.isEmpty()) { UI.dim("  No exams sat yet."); return; }
        String[] names = { "Fail", "Pass", "Merit", "Distinction" };
        for (var e : p.courseGrades.entrySet())
            UI.say("    " + pad(Data.COURSES.get(e.getKey()).name, 30) + names[e.getValue()]);
        UI.info("  GPA: " + String.format("%.2f", p.gpa()) + " (" + p.gpaTier() + ")");
    }

    // ---------------------------------------------------------------- shop (§12)

    static int dealPrice(Data.Item it) {
        return isDeal(it.key) ? (int) Math.round(it.cost * 0.9) : it.cost;
    }

    static boolean isDeal(String key) {
        List<String> keys = new ArrayList<>(Data.ITEMS.keySet());
        Random seeded = new Random(LocalDate.now().toEpochDay());
        int a = seeded.nextInt(keys.size());
        int b = seeded.nextInt(keys.size() - 1);
        if (b >= a) b++;
        return keys.get(a).equals(key) || keys.get(b).equals(key);
    }

    static void shop() {
        UI.info("  THE SHOP -- every purchase is comfort now versus a door later.");
        for (Data.Item it : Data.ITEMS.values()) {
            if (p.level < it.minLevel) continue;
            String price = isDeal(it.key) ? "EUR " + dealPrice(it) + " (deal!)" : "EUR " + it.cost;
            String owned = p.has(it.key) ? " [owned" + (p.inventory.get(it.key) > 1 ? " x" + p.inventory.get(it.key) : "") + "]" : "";
            UI.say("    " + pad(it.key, 14) + pad(price, 18) + it.desc + owned);
        }
        if (p.laptopTier < 3 && p.level >= Data.LAPTOP_MINLV[p.laptopTier + 1])
            UI.say("    " + pad("laptop", 14) + pad("EUR " + Data.LAPTOP_COST[p.laptopTier + 1], 18)
                + "upgrade: " + Data.LAPTOP_NAMES[p.laptopTier + 1] + " (`upgrade laptop`)");
        for (int t = p.rideTier + 1; t <= 3; t++) {
            if (p.level < Data.RIDE_MINLV[t]) break;
            int tradeIn = Data.RIDE_COST[p.rideTier] / 2;
            UI.say("    " + pad(rideKey(t), 14) + pad("EUR " + (Data.RIDE_COST[t] - tradeIn), 18)
                + Data.RIDE_NAMES[t] + " -- work burnout -" + Data.RIDE_BURN[t]
                + (tradeIn > 0 ? " (trade-in applied)" : ""));
        }
        UI.dim("  buy <thing> | inspect <thing> | deals");
    }

    static String rideKey(int tier) { return new String[]{"shoes", "bike", "startercar", "reliablecar"}[tier]; }

    static void deals() {
        UI.info("  Today's deals (10% off):");
        for (Data.Item it : Data.ITEMS.values())
            if (isDeal(it.key) && p.level >= it.minLevel)
                UI.say("    " + pad(it.key, 14) + "EUR " + dealPrice(it) + "  (was " + it.cost + ")");
    }

    static void buy(String arg) {
        if (arg.isEmpty()) { shop(); return; }
        String a = arg.toLowerCase().replace(" ", "");
        // transport line
        for (int t = 1; t <= 3; t++) {
            if (!rideKey(t).equals(a) && !Data.RIDE_NAMES[t].toLowerCase().replace(" ", "").contains(a)) continue;
            if (t <= p.rideTier) { UI.dim("  You've moved past that."); return; }
            if (p.level < Data.RIDE_MINLV[t]) { UI.warn("  Not yet. (level " + Data.RIDE_MINLV[t] + ")"); return; }
            int cost = Data.RIDE_COST[t] - Data.RIDE_COST[p.rideTier] / 2;
            if (p.money < cost) { UI.warn("  EUR " + cost + " needed (after trade-in). You have " + p.money + "."); return; }
            p.addMoney(-cost);
            p.rideTier = t;
            UI.ok("  " + Data.RIDE_NAMES[t] + " acquired. EUR -" + cost);
            UI.narrate(Data.RIDE_BUYLINE[t]);
            return;
        }
        if (a.equals("laptop") || a.equals("thinkpad")) { upgrade("laptop"); return; }
        Data.Item it = findItem(a);
        if (it == null) { UI.warn("  No such item."); return; }
        if (p.level < it.minLevel) { UI.warn("  Not available yet."); return; }
        if (it.requires != null && !p.has(it.requires)) {
            UI.warn("  Needs: " + Data.ITEMS.get(it.requires).name + " first."); return;
        }
        if (!it.consumable && p.has(it.key)) { UI.dim("  Already own one. It's doing its best."); return; }
        int cost = dealPrice(it);
        if (p.money < cost) { UI.warn("  EUR " + cost + " needed. You have " + p.money + "."); return; }
        p.addMoney(-cost);
        boolean first = !p.inventory.containsKey(it.key);
        p.inventory.merge(it.key, 1, Integer::sum);
        UI.ok("  " + it.name + " bought. EUR -" + cost);
        if (first) UI.narrate(it.copy);
        checkAchievements();
    }

    static Data.Item findItem(String a) {
        for (Data.Item it : Data.ITEMS.values())
            if (it.key.equals(a) || it.name.toLowerCase().replace(" ", "").contains(a)) return it;
        return null;
    }

    static void use(String arg) {
        Data.Item it = findItem(arg.toLowerCase().replace(" ", ""));
        if (it == null || !p.has(it.key)) { UI.warn("  You don't have that."); return; }
        if (!it.consumable) { UI.dim("  It works by existing. No need to use it."); return; }
        p.inventory.merge(it.key, -1, Integer::sum);
        if (p.inventory.get(it.key) <= 0) p.inventory.remove(it.key);
        switch (it.key) {
            case "coffee":
                p.addStamina(20); p.addBurnout(-8);
                UI.ok("  Coffee: +20 stamina, -8 burnout."); break;
            case "energydrink":
                p.addStamina(40); p.addBurnout(8);
                UI.ok("  Energy drink: +40 stamina, +8 burnout. Future you noticed."); break;
            case "onlinecourse":
                p.skillPoints += 5;
                UI.ok("  Online course finished. +5 skill points. Module 7 was a YouTube video."); break;
        }
    }

    static void sell(String arg) {
        String[] parts = arg.split("\\s+");
        if (parts[0].isEmpty()) { UI.dim("  sell <item> [qty]"); return; }
        Data.Item it = findItem(parts[0].toLowerCase());
        if (it == null || !p.has(it.key)) { UI.warn("  You don't have that."); return; }
        int qty = 1;
        if (parts.length > 1) try { qty = Math.max(1, Integer.parseInt(parts[1])); } catch (NumberFormatException ignored) {}
        qty = Math.min(qty, p.inventory.get(it.key));
        int value = it.cost / 2 * qty;
        p.inventory.merge(it.key, -qty, Integer::sum);
        if (p.inventory.get(it.key) <= 0) p.inventory.remove(it.key);
        p.addMoney(value);
        UI.ok("  Sold " + qty + "x " + it.name + " for EUR " + value + ".");
    }

    static void inspect(String arg) {
        String a = arg.toLowerCase().replace(" ", "");
        Data.Item it = findItem(a);
        if (it != null) {
            UI.info("  " + it.name + " -- EUR " + dealPrice(it));
            UI.say("    " + it.desc);
            UI.dim("    \"" + it.copy + "\"");
            return;
        }
        for (int t = 1; t <= 3; t++) if (rideKey(t).equals(a)) {
            UI.info("  " + Data.RIDE_NAMES[t] + " -- EUR " + Data.RIDE_COST[t]);
            UI.say("    Work burnout -" + Data.RIDE_BURN[t] + (t == 2 ? ". Adds the NCT event." : t == 3 ? ". Immune to the NCT event." : "."));
            UI.dim("    \"" + Data.RIDE_COPY[t] + "\"");
            return;
        }
        if (a.equals("laptop")) {
            UI.info("  " + Data.LAPTOP_NAMES[p.laptopTier] + " (current)");
            UI.dim("    \"" + Data.LAPTOP_COPY[p.laptopTier] + "\"");
            if (p.laptopTier < 3) UI.say("    Next: " + Data.LAPTOP_NAMES[p.laptopTier + 1] + ", EUR "
                + Data.LAPTOP_COST[p.laptopTier + 1] + " (level " + Data.LAPTOP_MINLV[p.laptopTier + 1] + "+)");
            return;
        }
        UI.warn("  Nothing by that name.");
    }

    static void upgrade(String arg) {
        if (!arg.toLowerCase().startsWith("laptop")) { UI.dim("  upgrade laptop"); return; }
        if (p.laptopTier >= 3) { UI.dim("  The Dream Machine is already yours. The guilt: ongoing."); return; }
        int next = p.laptopTier + 1;
        if (p.level < Data.LAPTOP_MINLV[next]) { UI.warn("  " + Data.LAPTOP_NAMES[next] + " needs level " + Data.LAPTOP_MINLV[next] + "."); return; }
        int cost = Data.LAPTOP_COST[next];
        if (p.money < cost) { UI.warn("  EUR " + cost + " needed. You have " + p.money + "."); return; }
        p.addMoney(-cost);
        p.laptopTier = next;
        UI.ok("  " + Data.LAPTOP_NAMES[next] + " acquired. EUR -" + cost + " (+" + Data.LAPTOP_BONUS[next] + "/work)");
        UI.narrate(Data.LAPTOP_COPY[next]);
        if (next == 1) Story.hook("eulogy");
        checkAchievements();
    }

    // ---------------------------------------------------------------- skills & career

    static void skillsCmd() {
        UI.info("  Skills (+EUR 2/level on matching jobs). learnskill <name> costs 5 SP. SP: " + p.skillPoints);
        for (String s : new String[]{"basics", "backend", "frontend", "devops", "ml"})
            UI.say("    " + pad(s, 10) + "Lv " + p.skill(s));
    }

    static void learnSkill(String arg) {
        String s = arg.toLowerCase().trim();
        if (!List.of("basics", "backend", "frontend", "devops", "ml").contains(s)) {
            UI.warn("  Skills: basics, backend, frontend, devops, ml"); return;
        }
        if (p.skillPoints < 5) { UI.warn("  Needs 5 skill points. You have " + p.skillPoints + ". (`learn`)"); return; }
        p.skillPoints -= 5;
        p.skills.merge(s, 1, Integer::sum);
        UI.ok("  " + s + " -> Lv " + p.skill(s));
        checkAchievements();
    }

    static void pathCmd() {
        if (p.careerPath.isEmpty()) UI.dim("  No senior path chosen. `choose <backend|frontend|ai|devops>` at level 41+.");
        else UI.info("  Career path: " + p.careerPath + " (+EUR 40 on matching jobs)");
    }

    static void choosePath(String arg) {
        if (p.level < 41) { UI.warn("  Senior career paths open at level 41."); return; }
        if (!p.careerPath.isEmpty()) { UI.dim("  Chosen: " + p.careerPath + ". The story acknowledges pivots; the payroll system doesn't."); return; }
        String s = arg.toLowerCase().trim();
        if (!List.of("backend", "frontend", "ai", "devops").contains(s)) {
            UI.warn("  choose <backend|frontend|ai|devops>"); return;
        }
        p.careerPath = s;
        p.quests.addAll(Data.pathQuests(s));
        UI.ok("  Path chosen: " + s + ". Two new quests. +EUR 40 on matching jobs.");
        if (!p.flag("gradPath").isEmpty() && !p.flag("gradPath").startsWith(s))
            UI.narrate("You started in " + p.flag("gradPath") + " and ended up in " + s + ". Careers are like that.");
        award("path_chosen", "Chose a Career Path");
    }

    // ---------------------------------------------------------------- quests & achievements

    static void questProgress(String action) {
        for (Data.Quest q : p.quests) {
            if (q.done || !q.action.equals(action) || p.level < q.minLevel) continue;
            q.progress++;
            if (q.progress >= q.target) completeQuest(q);
        }
    }

    static void questPortfolioCheck() {
        for (Data.Quest q : p.quests)
            if (!q.done && q.action.equals("portfolio") && p.portfolio >= q.target) {
                q.progress = q.target;
                completeQuest(q);
            }
    }

    static void completeQuest(Data.Quest q) {
        q.done = true;
        p.addMoney(q.rewardMoney());
        UI.gold("  QUEST COMPLETE: " + q.name + "  (EUR +" + q.rewardMoney() + ", +25 XP)");
        Story.hook("firstQuest");
        award("quest_complete", "Quest Completed");
        gainXp(25);
    }

    static void questsCmd() {
        boolean any = false;
        for (Data.Quest q : p.quests) {
            if (p.level < q.minLevel || q.done) continue;
            any = true;
            String bar = q.action.equals("portfolio")
                ? Math.min(p.portfolio, q.target) + "/" + q.target + " portfolio"
                : q.progress + "/" + q.target + " " + q.action;
            UI.say("    " + pad(q.name, 42) + bar);
        }
        long done = p.quests.stream().filter(q -> q.done).count();
        if (!any) UI.dim("  No active quests at this level.");
        UI.dim("  Completed: " + done + "/" + p.quests.size());
    }

    static final String[][] ACHIEVEMENTS = {
        {"first_euro", "First Euro"}, {"hundred_euros", "EUR 100"}, {"five_hundred", "EUR 500"},
        {"thousand_euros", "EUR 1,000"}, {"five_thousand", "EUR 5,000 -- runway"},
        {"portfolio_20", "Portfolio 20"}, {"portfolio_50", "Portfolio 50"}, {"portfolio_100", "Portfolio 100"},
        {"laptop_lv1", "RIP the Hairdryer"}, {"graduate", "Graduate"}, {"senior", "Reached Senior"},
        {"path_chosen", "Chose a Career Path"}, {"quest_complete", "Quest Completed"},
        {"level_20", "Level 20"}, {"polyglot", "Polyglot"}, {"distinction", "First Distinction"},
        {"survived_pip", "Survived a PIP"}, {"two_fourteen_am", "2:14 AM"},
    };

    static void award(String id, String name) {
        if (p.achieved.contains(id)) return;
        p.achieved.add(id);
        UI.gold("  ACHIEVEMENT: " + name);
    }

    static void checkAchievements() {
        if (p.money >= 1) award("first_euro", "First Euro");
        if (p.money >= 100) award("hundred_euros", "EUR 100");
        if (p.money >= 500) award("five_hundred", "EUR 500");
        if (p.money >= 1000) award("thousand_euros", "EUR 1,000");
        if (p.money >= 5000) award("five_thousand", "EUR 5,000 -- runway");
        if (p.portfolio >= 20) award("portfolio_20", "Portfolio 20");
        if (p.portfolio >= 50) award("portfolio_50", "Portfolio 50");
        if (p.portfolio >= 100) award("portfolio_100", "Portfolio 100");
        if (p.laptopTier >= 1) award("laptop_lv1", "RIP the Hairdryer");
        if (p.graduated) award("graduate", "Graduate");
        if (p.level >= 41) { award("senior", "Reached Senior"); if (p.title.isEmpty()) p.title = "Senior Dev"; }
        if (p.level >= 20) award("level_20", "Level 20");
        int skillsKnown = 0;
        for (int v : p.skills.values()) if (v >= 1) skillsKnown++;
        if (skillsKnown >= 3) award("polyglot", "Polyglot");
        for (int g : p.courseGrades.values()) if (g == 3) { award("distinction", "First Distinction"); break; }
    }

    static void achievementsCmd() {
        for (String[] a : ACHIEVEMENTS)
            UI.say("    " + (p.achieved.contains(a[0]) ? "[x] " : "[ ] ") + a[1]);
    }

    // ---------------------------------------------------------------- info commands

    static void status() {
        UI.info("  " + p.name + (p.title.isEmpty() ? "" : " -- " + p.title));
        UI.say("    Phase     " + p.phaseLabel());
        UI.say("    Level     " + p.level + "  (" + p.xp + "/" + p.xpToNext() + " XP)");
        UI.say("    Money     EUR " + p.money);
        UI.say("    Stamina   " + p.stamina + "/" + p.staminaMax);
        UI.say("    Burnout   " + p.burnout + "/100" + (p.burnout >= 80 ? "  [WORK LOCKED]" : p.burnout >= 50 ? "  [gains reduced]" : ""));
        UI.say("    SP        " + p.skillPoints + "   Portfolio " + p.portfolio + "   Reputation " + p.reputation);
        UI.say("    Laptop    " + Data.LAPTOP_NAMES[p.laptopTier] + (p.loaner ? " + Company Loaner" : ""));
        UI.say("    Transport " + Data.RIDE_NAMES[p.rideTier]);
        Data.Job j = currentJob();
        UI.say("    Job       " + (j == null ? "none available" : j.name + " (EUR ~" + earnings(j) + "/work)"));
        if (p.level <= 16) UI.say("    GPA       " + (p.courseGrades.isEmpty() ? "--" : String.format("%.2f", p.gpa())));
        if (p.phase().equals("INTERN")) {
            UI.say("    Company   " + Story.coName() + "   Culture " + p.culture);
            if (p.isTrue("reviewRevealed")) UI.say("    Review    " + Story.reviewScore() + "  (return offer ~65+, the theatre is going " + (Story.reviewScore() >= 65 ? "well" : Story.reviewScore() >= 40 ? "okay" : "badly") + ")");
        }
        if (p.level >= 25) UI.say("    Imposter  " + p.imposter + "/100" + (p.imposter >= 85 ? "  [productivity penalty]" : ""));
        if (!p.careerPath.isEmpty()) UI.say("    Path      " + p.careerPath);
        if (p.prestige > 0) UI.say("    Tellings  " + p.prestige + "  (+" + (p.prestige * 5) + "% Embellishment)");
    }

    static void inventory() {
        if (p.inventory.isEmpty()) { UI.dim("  Empty. Like all the best starts."); return; }
        for (var e : p.inventory.entrySet()) {
            Data.Item it = Data.ITEMS.get(e.getKey());
            UI.say("    " + pad(it == null ? e.getKey() : it.name, 30) + (e.getValue() > 1 ? "x" + e.getValue() : ""));
        }
    }

    static void jobs() {
        UI.info("  Jobs available now (setjob <name>):");
        for (Data.Job j : Data.JOBS) {
            if (Data.phaseRank(j.phase) > Data.phaseRank(p.phase())) continue;
            boolean laptopOk = !j.needsLaptop || p.laptopTier >= 1 || p.loaner;
            String mark = j.name.equalsIgnoreCase(p.job) ? " <- current" : "";
            String lock = laptopOk ? "" : "  [needs a real laptop]";
            UI.say("    " + pad(j.name, 28) + pad("EUR " + j.pay, 10) + pad(j.xp + " XP", 8) + j.phase + lock + mark);
        }
    }

    static void setjob(String arg) {
        if (arg.isEmpty()) { jobs(); return; }
        for (Data.Job j : Data.JOBS) {
            if (!j.name.toLowerCase().contains(arg.toLowerCase())) continue;
            if (Data.phaseRank(j.phase) > Data.phaseRank(p.phase())) { UI.warn("  Not at your phase yet."); return; }
            if (j.needsLaptop && p.laptopTier < 1 && !p.loaner) { UI.warn("  That one needs a real laptop. The Hairdryer doesn't count."); return; }
            p.job = j.name;
            UI.ok("  Job set: " + j.name);
            return;
        }
        UI.warn("  No job matches that.");
    }

    static void help() {
        UI.info("  CORE      work(w) study(s) learn project(p) team(t) rest(r) sideproject");
        UI.info("  COLLEGE   attend <course>  courses  gpa");
        UI.info("  INFO      status(st) inventory(i) jobs(j) quests(q) achievements(ach) skills path");
        UI.info("  SHOP      shop buy use sell inspect deals  |  upgrade laptop");
        UI.info("  CAREER    setjob <name>  learnskill <skill>  choose <path>  doors(50+)");
        UI.info("  META      tutorial prestige event save load saves export quit");
    }

    static void tutorial() {
        if (p.tutorialDone) { UI.dim("  Already done. The EUR 20 was a one-time act of generosity."); return; }
        p.tutorialDone = true;
        p.addMoney(20);
        UI.scene("The loop: `work` for money, `study` for XP, `attend <course>` for college,",
                 "`rest` before burnout locks you out. Level up, and the story finds you.",
                 "Your exams are real CS questions. The game assumes you can take them.",
                 "EUR 20 starter bonus. Spend it on coffee or save it. Everything is that choice, forever.");
        checkAchievements();
    }

    static void prestigeCmd() {
        if (p.flag("ending").isEmpty()) {
            UI.warn("  The story isn't over. Prestige unlocks after the Three Doors (level 50).");
            return;
        }
        int c = UI.choose("Tell the story again? (Level 1, everything reset; +5% Embellishment, perks and achievements survive)",
            "Tell it again", "Not tonight");
        if (c == 1) return;
        Player old = p;
        p = new Player();
        Story.p = p;
        p.name = old.name;
        p.prestige = old.prestige + 1;
        p.achieved = old.achieved;
        p.title = old.title;
        for (String perk : new String[]{"scarTissue1", "scarTissue2", "founderScar"})
            if (old.isTrue(perk)) p.set(perk, "true");
        if (p.prestige == 1)
            UI.scene("You told the story again the other night. Different pub, same question.",
                     "It came out better the second time. Stories do. Nobody minds.",
                     "(Prestige renamed: Embellishment. +5% money per telling.)");
        UI.gold("  Telling #" + (p.prestige + 1) + ". +" + (p.prestige * 5) + "% Embellishment.");
        Story.prologue();
    }

    /** Dev/testing tool, like `event`: dev xp <n> | money <n> | grade <course> <0-3> | flag <k> <v> | restore */
    static void devCmd(String arg) {
        String[] a = arg.split("\\s+");
        try {
            switch (a[0]) {
                case "xp": gainXp(Integer.parseInt(a[1])); afterAction(false); break;
                case "money": p.addMoney(Integer.parseInt(a[1])); checkAchievements(); break;
                case "grade":
                    Data.Course c = findCourse(a[1]);
                    p.courseGrades.put(c.key, Integer.parseInt(a[2]));
                    UI.dim("  " + c.name + " grade set: " + a[2]);
                    Story.checkGates();
                    break;
                case "flag": p.set(a[1], a[2]); UI.dim("  flag " + a[1] + "=" + a[2]); break;
                case "restore": p.stamina = p.staminaMax; p.burnout = 0; UI.dim("  restored"); break;
                default: UI.dim("  dev xp|money|grade|flag|restore");
            }
        } catch (Exception e) {
            UI.dim("  dev xp|money|grade|flag|restore -- " + e);
        }
    }

    static void doorsCmd() {
        if (p.level < 50) { UI.dim("  Level 50 first. The doors aren't going anywhere."); return; }
        Story.doors();
    }

    // ---------------------------------------------------------------- persistence commands

    static void autosave() {
        try { p.save(DEFAULT_SAVE); } catch (IOException ignored) {}
    }

    static void saveCmd(String arg) {
        Path f = arg.isEmpty() ? DEFAULT_SAVE : Path.of(arg.endsWith(".txt") ? arg : arg + ".txt");
        try { p.save(f); UI.ok("  Saved: " + f); } catch (IOException e) { UI.err("  Save failed: " + e.getMessage()); }
    }

    static void loadCmd(String arg) {
        Path f = arg.isEmpty() ? DEFAULT_SAVE : Path.of(arg.endsWith(".txt") ? arg : arg + ".txt");
        try {
            p = Player.load(f);
            Story.p = p;
            UI.ok("  Loaded: " + f + " (Lv " + p.level + ", EUR " + p.money + ")");
        } catch (IOException e) { UI.err("  Load failed: " + e.getMessage()); }
    }

    static void savesCmd() {
        try (DirectoryStream<Path> dir = Files.newDirectoryStream(Path.of("."), "*.txt")) {
            for (Path f : dir) UI.say("    " + f.getFileName());
        } catch (IOException e) { UI.err("  " + e.getMessage()); }
    }

    static void exportCmd(String arg) {
        Path f = Path.of(arg.isEmpty() ? "stats.csv" : arg);
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(f, StandardCharsets.UTF_8))) {
            w.println("name,level,xp,money,stamina,burnout,portfolio,skillPoints,reputation,prestige,phase");
            w.println(p.name + "," + p.level + "," + p.xp + "," + p.money + "," + p.stamina + ","
                + p.burnout + "," + p.portfolio + "," + p.skillPoints + "," + p.reputation + ","
                + p.prestige + "," + p.phase());
            UI.ok("  Exported: " + f);
        } catch (IOException e) { UI.err("  Export failed: " + e.getMessage()); }
    }

    static void quit() {
        autosave();
        UI.dim("  Saved. The rain continues without you.");
        running = false;
    }

    static String pad(String s, int n) {
        if (s.length() >= n) return s + " ";
        return s + " ".repeat(n - s.length());
    }
}
