import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Random events: ~18% chance after each action. Quiz and recruiter re-fire the course banks. */
class Events {
    static final Random RNG = new Random();

    /** Ask one bank question (shuffled options). Returns true if correct. */
    static boolean ask(Data.Question q) {
        List<String> opts = new ArrayList<>(List.of(q.answer, q.wrong1, q.wrong2));
        Collections.shuffle(opts, RNG);
        int pick = UI.choose(q.prompt, opts.toArray(new String[0]));
        boolean right = opts.get(pick).equals(q.answer);
        if (right) UI.ok("  Correct. " + q.resultLine);
        else UI.err("  Wrong -- it's: " + q.answer + ". " + q.resultLine);
        return right;
    }

    /** Pool of questions from courses the player has actually sat exams in. */
    static List<Data.Question> completedBankPool(Player p) {
        List<Data.Question> pool = new ArrayList<>();
        for (String key : p.courseGrades.keySet()) {
            Data.Course c = Data.COURSES.get(key);
            if (c != null) Collections.addAll(pool, c.bank);
        }
        return pool;
    }

    static void maybeFire(Player p) {
        if (RNG.nextDouble() >= 0.18) return;
        fire(p, null);
    }

    /** Fire a random event; forced (non-null) picks by name for the dev `event` command. */
    static void fire(Player p, String forced) {
        List<String> pool = new ArrayList<>(List.of("hacker", "freelance", "quiz"));
        if (p.level <= 16) pool.add("exam");
        if (p.level >= 17) { pool.add("outage"); pool.add("recruiter"); }
        if (p.level >= 25 && p.rideTier == 2) pool.add("nct");

        String pick = forced != null && pool.contains(forced) ? forced : pool.get(RNG.nextInt(pool.size()));
        UI.blank();
        UI.warn("  *** EVENT ***");
        switch (pick) {
            case "hacker":    hacker(p); break;
            case "freelance": freelance(p); break;
            case "quiz":      quiz(p); break;
            case "exam":      examCram(p); break;
            case "outage":    outage(p); break;
            case "recruiter": recruiter(p); break;
            case "nct":       nct(p); break;
        }
    }

    static void hacker(Player p) {
        UI.say("  Someone is poking at your accounts. Login attempt from an IP that is not you.");
        int c = UI.choose("Defend?", "DEFEND -- change passwords, check sessions", "IGNORE -- probably nothing");
        if (c == 0) {
            int defence = 50 + (p.has("raspberrypi") ? 25 : 0) + p.skill("devops") * 5;
            if (RNG.nextInt(100) < defence) {
                UI.ok("  Locked out. The Pi earns its keep as more than a paperweight.");
                RPG.gainXp(12);
                UI.info("  +12 XP");
            } else {
                int loss = Math.min(p.money, 30);
                p.addMoney(-loss);
                UI.err("  Too slow. They got into a delivery account. EUR -" + loss);
                p.imposterBump(8);
            }
        } else {
            int loss = Math.min(p.money, 40);
            p.addMoney(-loss);
            UI.err("  It was not nothing. EUR -" + loss);
        }
    }

    static void freelance(Player p) {
        int pay = 25 + p.level * 3;
        UI.say("  A friend of a friend needs 'a quick website thing'. EUR " + pay + ", tonight.");
        int c = UI.choose("Take it?", "ACCEPT -- it's never quick, but it's money", "DECLINE -- sleep is also money");
        if (c == 0) {
            p.addMoney(pay);
            RPG.gainXp(10);
            p.addStamina(-15);
            p.addBurnout(4);
            UI.ok("  Done by 2am. 'One more small change?' EUR +" + pay + ", +10 XP, -15 stamina");
        } else {
            UI.dim("  They found someone on Fiverr. Godspeed, someone on Fiverr.");
        }
    }

    static void quiz(Player p) {
        List<Data.Question> pool = completedBankPool(p);
        if (pool.isEmpty()) Collections.addAll(pool, Data.COURSES.get("cs101").bank);
        UI.say("  Pop quiz. No stakes. Except pride.");
        Data.Question q = pool.get(RNG.nextInt(pool.size()));
        if (ask(q)) { RPG.gainXp(10); UI.info("  +10 XP"); }
        else p.imposterBump(5);
    }

    static void examCram(Player p) {
        UI.say("  An exam looms. Everyone in the library looks like they've seen something terrible.");
        int c = UI.choose("Tonight:", "CRAM -- +XP, -stamina", "REST -- +stamina, the notes will still be there");
        if (c == 0) {
            RPG.gainXp(14);
            p.addStamina(-18);
            UI.ok("  +14 XP, -18 stamina. The flashcards blur around 1am.");
        } else {
            p.addStamina(20);
            UI.ok("  +20 stamina. Rest is also a strategy.");
        }
    }

    static void outage(Player p) {
        UI.say("  INCIDENT: production is down. The group chat is just people typing '...'");
        int c = UI.choose("Jump in?", "HELP -- money + XP, -stamina", "SKIP -- someone else will get it");
        if (c == 0) {
            int pay = 30 + p.level * 2;
            p.addMoney(pay);
            RPG.gainXp(15);
            p.addStamina(-20);
            p.reputation += 1;
            UI.ok("  You find it. A config change. It's always a config change. EUR +" + pay + ", +15 XP");
        } else {
            UI.dim("  Someone else got it. They will mention this for months.");
        }
    }

    static void recruiter(Player p) {
        UI.say("  A recruiter calls. 'Exciting opportunity.' There is a tech screen, right now.");
        int c = UI.choose("Take the screen?", "YES -- three questions, money if you pass", "NO -- you're driving / in a meeting / asleep");
        if (c == 1) { UI.dim("  'I'll circle back.' They will. They always do."); return; }
        List<Data.Question> pool = completedBankPool(p);
        if (pool.isEmpty()) Collections.addAll(pool, Data.COURSES.get("cs101").bank);
        Collections.shuffle(pool, RNG);
        UI.narrate("Brennan's voice in your head: read the loop before you answer.");
        int right = 0;
        for (int i = 0; i < Math.min(3, pool.size()); i++) if (ask(pool.get(i))) right++;
        if (right >= 2) {
            int pay = 40 + p.level * 4;
            p.addMoney(pay);
            RPG.gainXp(20);
            UI.ok("  Passed " + right + "/3. They'll 'be in touch about next steps'. A referral bonus lands anyway. EUR +" + pay + ", +20 XP");
        } else {
            UI.err("  " + right + "/3. 'We've decided to move forward with other candidates.'");
            p.imposterBump(8);
        }
    }

    static void nct(Player p) {
        UI.say("  The Starter Car is due its NCT. It makes a new sound on the way to the test centre.");
        if (RNG.nextInt(100) < 55) {
            p.addMoney(55);
            UI.ok("  It passes. Unearned smugness, and the EUR 55 you'd mentally spent on repairs. EUR +55");
        } else {
            int cost = Math.min(p.money, 350);
            p.addMoney(-cost);
            UI.err("  Failed. The ball joint. It's always the ball joint. EUR -" + cost);
        }
    }
}
