import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * StoryEngine: one spine of beats triggered by level, flags as memory, doors as branching.
 * All copy is from the Story Bible (2026-06-11). Beats fire once (B| lines in the save).
 */
class Story {
    static Player p;
    static final Random RNG = new Random();

    // ---------------------------------------------------------------- helpers

    /** Run a beat once. */
    static boolean beat(String id, Runnable scene) {
        if (p.seen(id)) return false;
        p.beatsSeen.add(id);
        scene.run();
        return true;
    }

    static String name() { return p.name; }

    // ---------------------------------------------------------------- prologue & cards

    static void prologue() {
        UI.card("",
            "Someone asked me yesterday how I got into tech.",
            "I gave them the short version. Everyone gives the short version.",
            "",
            "This is the long one. Ten years. Four jobs.",
            "One genuinely terrible Tuesday.",
            "",
            "It starts where these stories always start:",
            "no money, a laptop that sounds like a hairdryer, and the rain.");
        UI.card("CHAPTER ONE -- FRESHERS",
            "Dublin. September. Raining like it's being paid to.",
            "You're a first-year CS student with nothing to your name,",
            "a flatmate who's smarter than you,",
            "and a creeping suspicion that everyone else got a manual you didn't.");
    }

    // ---------------------------------------------------------------- level-up dispatch

    static void onLevelUp(int lv) {
        switch (lv) {
            case 2:  beat("1.1", Story::b1_1); break;
            case 5:  beat("year2", () -> UI.card("YEAR TWO",
                        "September again. The rain hasn't changed. You have. Slightly.")); break;
            case 6:  beat("1.2", Story::b1_2); break;
            case 9:  beat("year3", () -> UI.card("YEAR THREE",
                        "Halfway. The modules have stopped being about computers",
                        "and started being about maths wearing a computer costume.")); break;
            case 10: beat("1.3", Story::b1_3); break;
            case 11: beat("1.4", Story::b1_4); break;
            case 13: beat("year4", () -> UI.card("YEAR FOUR",
                        "Final year. Everyone's CV says \"proficient in Java.\"",
                        "Everyone is lying by at least one adverb.")); break;
            case 14: if (p.isTrue("brennanNoticed") || p.courseGrades.getOrDefault("algorithms", -1) == 3)
                         beat("1.5", Story::b1_5);
                     break;
            case 15: beat("1.6", Story::b1_6); break;
            case 17: beat("ch2open", Story::ch2open); beat("2.1", Story::b2_1); break;
            case 18: beat("2.2", Story::b2_2); break;
            case 19: beat("2.3", Story::b2_3); break;
            case 20: beat("2.4", Story::b2_4); break;
            case 21: beat("2.5", Story::b2_5); break;
            case 22: if (p.is("aoifeState", "gone")) beat("2.3b", Story::aoifeQuits); break;
            case 23: beat("2.6", Story::b2_6); break;
            case 24: beat("2.7", Story::b2_7); break;
            case 25: beat("ch3", Story::ch3open); beat("3.1", Story::b3_1); break;
            case 28: beat("3.2", Story::b3_2); break;
            case 31: beat("3.3", Story::b3_3); break;
            case 33: beat("3.4", Story::b3_4); break;
            case 36: beat("3.5", Story::b3_5); break;
            case 38: if (p.isTrue("joinedSelkie")) beat("3.6", Story::b3_6); break;
            case 40: beat("3.7", Story::b3_7); break;
            case 41: beat("ch4", Story::ch4open); break;
            case 42: trackBeat("I4.1", Story::i4_1, "M4.1", Story::m4_1); break;
            case 44: trackBeat("I4.2", Story::i4_2, "M4.2", Story::m4_2); break;
            case 46: trackBeat("I4.3", Story::i4_3, "M4.3", Story::m4_3); break;
            case 48: trackBeat("I4.4", Story::i4_4, "M4.4", Story::m4_4); break;
            case 49: trackBeat("I4.5", Story::i4_5, "M4.5", Story::m4_5); break;
            case 50: beat("finale", Story::finale); break;
        }
    }

    static void trackBeat(String icId, Runnable ic, String mgmtId, Runnable mgmt) {
        if (p.is("seniorTrack", "mgmt")) beat(mgmtId, mgmt);
        else beat(icId, ic);
    }

    /** Gates checked after actions/exams (not tied to a level-up). */
    static void checkGates() {
        if (p.level == 16 && !p.graduated && p.coreCoursesPassed()) beat("1.7", Story::graduation);
    }

    // ---------------------------------------------------------------- mechanic hooks (§10)

    static void hook(String id) {
        switch (id) {
            case "wall":
                beat("hook_wall", () -> {
                    UI.scene("Nobody tells you burnout doesn't feel like fire.",
                             "It feels like nothing.");
                    if (p.is("aoifeState", "ally"))
                        UI.narrate("\"Go home, " + name() + ".\" -- her words, ten years later, still working.");
                });
                break;
            case "fraud":
                if (p.imposter >= 100) beat("hook_fraud", () -> {
                    String who = p.is("aoifeState", "ally") ? "AOIFE" : "MARCUS";
                    UI.scene(who + ": \"Everyone's winging it. The seniors are just winging it",
                             "in a calmer voice.\"");
                    p.imposter = 60;
                });
                break;
            case "eulogy":
                beat("hook_eulogy", () -> UI.narrate(
                    "RIP the hairdryer, 2016-2018. It died doing what it loved: thermal throttling."));
                break;
            case "firstQuest":
                beat("hook_quest", () -> UI.narrate("Small things first. That's the whole trick."));
                break;
            case "distinction":
                beat("hook_distinction", () -> UI.scene(
                    "BRENNAN (passing in the corridor): \"Don't let it go to your head.\""));
                break;
            case "firstPrestige":
                beat("hook_prestige", () -> UI.scene(
                    "You told the story again the other night. Different pub, same question.",
                    "It came out better the second time. Stories do. Nobody minds.",
                    "(Prestige bonus renamed: Embellishment. +5% per telling.)"));
                break;
        }
    }

    // ---------------------------------------------------------------- chapter 1

    static void b1_1() {
        UI.scene("Your flatmate Ciaran is playing a roguelike at 3pm.",
                 "He has a Web Fundamentals lab due at 5pm. He knows. He doesn't mind.",
                 "CIARAN: \"Want to know the secret to college, " + name() + "?",
                 "Attendance is a scam invented by people who own lecture halls.\"");
    }

    static void b1_2() {
        UI.scene("Tuesday, 2pm. Ciaran is in the kitchen, eating your bread.",
                 "CIARAN: \"I'm done. Dropping out.\"",
                 "YOU: \"...with the bread, or with college?\"",
                 "CIARAN: \"College is a queue, " + name() + ". A four-year queue. I'm skipping it.\"");
        int c = UI.choose("Say:",
            "\"You'll regret it.\"",
            "\"Fair play. What's the plan?\"",
            "Say nothing. Give him the rest of the bread.");
        if (c == 0) {
            p.relAdd("ciaran", -1);
            UI.narrate("He shrugs. He'll remember that.");
        } else if (c == 1) {
            p.relAdd("ciaran", 1);
            UI.scene("CIARAN: \"No idea. That's the plan.\"");
        } else {
            p.relAdd("ciaran", 1);
            UI.narrate("Some conversations are bread.");
        }
    }

    static void b1_3() {
        UI.scene("Algorithms. Brennan stops mid-sentence and looks directly at you.",
                 "BRENNAN: \"You. Yes, you.\"");
        Data.Question q = Data.COURSES.get("algorithms").bank[RNG.nextInt(3)];
        if (Events.ask(q)) {
            p.set("brennanNoticed", "true");
            RPG.gainXp(15);
            UI.scene("BRENNAN: \"Hm. Adequate. Sit down.\"");
        } else {
            UI.scene("BRENNAN: \"That answer was O(wrong). Sit down.\"");
        }
    }

    static void b1_4() {
        UI.scene("HACK THE LIFFEY -- 24-hour campus hackathon. Free pizza, broken sleep.",
                 "Siobhan -- top of the class, you've never actually spoken -- walks straight over.",
                 "SIOBHAN: \"You. You're not useless. Team up?\"",
                 "It is phrased like a challenge. It is a challenge.");
        int c = UI.choose("Hackathon:",
            "Team with Siobhan",
            "Go solo",
            "Skip it -- sleep is free and pizza isn't worth it");
        if (c == 0) {
            p.set("siobhanState", "ally");
            p.portfolio += 4;
            UI.scene("Second place. Twenty-six hours, one shared brain cell by the end.",
                     "First place was a blockchain thing. First place is always a blockchain thing.");
        } else if (c == 1) {
            p.set("siobhanState", "rival");
            p.portfolio += 6;
            UI.scene("You build alone. It's good. Hers is better -- she wins outright.",
                     "SIOBHAN: \"Good effort.\"",
                     "(It does not feel good.)");
        } else {
            p.set("siobhanState", "neutral");
            p.addStamina(25);
            UI.narrate("Rest is also a strategy. That's the lie I told myself.");
        }
    }

    static void b1_5() {
        UI.scene("Office hours. You ask Brennan for a recommendation.",
                 "She looks at you for three full seconds.",
                 "BRENNAN: \"Your proofs are lazy and your loop invariants are an act of vandalism.\"",
                 "BRENNAN: \"...Yes, fine. You ask questions. Most of them don't ask anything.\"");
        p.set("brennanRec", "true");
    }

    static void b1_6() {
        String subject = fypSubject();
        UI.scene("Final Year Project demo day. Yours is " + subject + ".",
                 "Brennan is your second reader. She reads the whole thing. Of course she does.",
                 "BRENNAN: \"The idea is fine. The write-up is fine. The loop invariants are,",
                 "as ever, a crime scene. ...Well done, " + name() + ".\"");
        p.portfolio += 10;
        UI.info("  +10 portfolio");
    }

    static String fypSubject() {
        String bestCourse = null;
        int best = -1;
        for (var e : p.courseGrades.entrySet()) {
            if (e.getValue() > best && !e.getKey().equals("cs101")) { best = e.getValue(); bestCourse = e.getKey(); }
        }
        if (bestCourse == null) return "a web app held together by optimism";
        switch (bestCourse) {
            case "security":  return "a security thing -- you fuzz a router and find something you shouldn't";
            case "statsml":   return "an ML thing -- it predicts bus delays slightly worse than the bus app";
            case "networks":  return "a networking thing -- a mesh chat that works in exactly one stairwell";
            case "databases": return "a database thing -- a query planner visualiser three people will ever love";
            case "os":        return "a systems thing -- a toy scheduler that outschedules your actual life";
            case "webfund":   return "a web thing -- it's clean, it's fast, it has eleven users and one is your ma";
            case "softeng":   return "a tooling thing -- a linter for commit messages. The committee is divided";
            default:          return "an algorithms thing -- Brennan's influence, visible from space";
        }
    }

    // graduation (1.7) -- phase gate at 16
    static void graduation() {
        p.graduated = true;
        String tier = p.gpaTier();
        p.set("gpaTier", tier);
        UI.card("GRADUATION",
            "A gown that doesn't fit, a hat built to be thrown,",
            "and a piece of Latin that cost four years.",
            "GPA: " + String.format("%.2f", p.gpa()) + " (" + tier + ")");
        int c = UI.choose("Four years in. What are you, when someone asks?", Data.GRAD_LABELS);
        String path = Data.GRAD_PATHS[c];
        p.set("gradPath", path);
        switch (path) {
            case "developer": addSkill("backend", 1); addSkill("frontend", 1); break;
            case "security":  addSkill("devops", 1);  p.inventory.merge("raspberrypi", 1, Integer::sum);
                              UI.info("  A Raspberry Pi appears in your bag. You don't ask."); break;
            case "qa":        addSkill("basics", 1);  p.staminaMax = 110;
                              UI.info("  Stamina cap +10. Breaking things is restful, somehow."); break;
            case "data":      addSkill("ml", 1);      p.set("studyBonus", "true"); break;
            case "devops":    addSkill("devops", 1);  break;
            case "ai":        addSkill("ml", 1);      break;
        }
        // chapter 1 reflection
        List<String> r = new ArrayList<>();
        r.add("Four years.");
        if (tier.equals("high")) r.add("Turns out the trick was going to the thing.");
        if (tier.equals("low"))  r.add("The transcript is... a document that exists. The degree counts the same.");
        if (p.is("siobhanState", "ally"))  r.add("It was about finding the people who make you sharper.");
        if (p.is("siobhanState", "rival")) r.add("It was about learning who you measure yourself against.");
        if (p.relOf("ciaran") > 0) r.add("Ciaran texted from the airport: 'told you the queue was optional.'");
        if (p.relOf("ciaran") < 0) r.add("Ciaran didn't text. Fair enough.");
        UI.card("", r.toArray(new String[0]));
    }

    static void addSkill(String s, int d) {
        p.skills.merge(s, d, Integer::sum);
        UI.info("  +" + d + " " + s + " skill");
    }

    // ---------------------------------------------------------------- chapter 2

    static void ch2open() {
        UI.card("CHAPTER TWO -- THE REAL WORLD",
            "You own one suit jacket. It will be too warm for the office.",
            "Across the river, the glass boxes of Silicon Docks are hiring.",
            "Three doors. Very different rooms behind them.");
        boolean giga = p.is("gpaTier", "high") || p.isTrue("brennanRec");
        boolean pub  = !p.is("gpaTier", "low");
        List<String> opts = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        if (giga) { opts.add("GigaCorp -- Compensation: excellent. Snacks: legendary. Deploying code: requires a form, a ticket, and a small blood sacrifice."); keys.add("gigacorp"); }
        else UI.dim("  [LOCKED] GigaCorp -- they only interview on recommendation or a high GPA. Neither arrived.");
        if (pub) { opts.add("PublicSector -- Compensation: fine. Stability: geological. The core system predates the euro. Possibly the lira."); keys.add("publicsector"); }
        else UI.dim("  [LOCKED] PublicSector -- the application form had a minimum GPA and a fondness for paperwork.");
        opts.add("StartupX -- Compensation: exposure, mostly. You will ship to production on day one. Possibly by accident.");
        keys.add("startupx");
        int c = UI.choose("The offers on the table:", opts.toArray(new String[0]));
        String co = keys.get(c);
        p.set("internCompany", co);
        p.culture = co.equals("gigacorp") ? 55 : co.equals("publicsector") ? 50 : 45;
        UI.ok("  Signed. The internship starts Monday.");
    }

    static String coName() {
        switch (p.flag("internCompany")) {
            case "gigacorp":     return "GigaCorp";
            case "publicsector": return "PublicSector";
            default:             return "StartupX";
        }
    }

    static void b2_1() {
        String co = p.flag("internCompany");
        if (co.equals("gigacorp"))
            UI.scene("Day one at GigaCorp. Your badge photo is terrible. It will outlive you.",
                     "Three onboarding modules in, you learn the wifi password is rotated quarterly",
                     "by a team whose entire job is rotating it.");
        else if (co.equals("publicsector"))
            UI.scene("Day one at PublicSector. Your machine takes nine minutes to log in.",
                     "A man named Brendan shows you the tea situation with real ceremony.",
                     "The codebase is older than you. Treat it with respect, like a sleeping dog.");
        else
            UI.scene("Day one at StartupX. There is no onboarding. There is a Slack channel called #help",
                     "and a founder who says 'we move fast' four times before lunch.",
                     "You deploy to production at 4:50pm. Nobody stops you. Nobody could.");
        UI.scene("You meet AOIFE -- your mentor. Kind eyes, tired in a way that has settled in.",
                 "And MARCUS -- a senior. He looks at your laptop, then at you, with sympathy.",
                 "MARCUS: \"You'll see the same forty people at every company in this town",
                 "for the rest of your career. Dublin tech is a village. Wave at people.\"");
        if (p.rideTier == 1)
            UI.narrate("You cycled here. You arrive damp but on time. The damp is permanent. So is the on-time.");
        p.loaner = true;
        UI.info("  Company loaner laptop issued. (+EUR 15 per work while you keep it.)");
    }

    static void b2_2() {
        boolean warm = p.relOf("ciaran") > 0;
        UI.scene("An email lands. FROM: Ciaran.",
                 "SUBJECT: " + (warm ? "\"you still owe me bread\"" : "\"long time. need a favour.\""),
                 "He's founding something. He won't say what yet. He needs eyes on a pitch deck.",
                 "It is nineteen slides. Slide seven is just the word 'DISRUPTION'.");
        int c = UI.choose("The deck:",
            "Help him -- stay up, leave real comments (-15 stamina)",
            "Leave it on read");
        if (c == 0) {
            p.addStamina(-15);
            p.relAdd("ciaran", 1);
            UI.scene("You send back forty comments at 1am.",
                     "CIARAN: \"thirty-eight of these are insults\"",
                     "CIARAN: \"the other two are good. cheers.\"");
        } else {
            UI.narrate("The email sits there. Some doors close quietly.");
        }
    }

    static void b2_3() {
        UI.scene("7:30am. You're in early to look keen. Aoife's already at her desk --",
                 "same clothes as yesterday. Her status has said 'active' since Tuesday.");
        int c = UI.choose("You:",
            "Cover her standup and send her home",
            "Quietly tell her to go home",
            "Pretend not to notice. Not your business.");
        if (c == 0) {
            p.set("aoifeState", "ally");
            UI.scene("AOIFE: \"...Thanks, " + name() + ". Don't end up like me, yeah?\"");
        } else if (c == 1) {
            p.set("aoifeState", "neutral");
            UI.narrate("She nods. She doesn't go home. But she logs off by nine, which is something.");
        } else {
            p.set("aoifeState", "gone");
            UI.narrate("You get a coffee. The moment passes. Some moments don't come back.");
        }
    }

    static void aoifeQuits() {
        UI.scene("Aoife's desk is empty. A one-line goodbye email, sent at 6am.",
                 "MARCUS: \"Burnout. Saw it coming for months. Everyone did.\"",
                 "You lose your mentor. The XP was the smaller part of it.");
    }

    static void b2_4() {
        String co = p.flag("internCompany");
        if (co.equals("gigacorp")) {
            UI.scene("THE FORM. Your fix is one line. Shipping it requires Change Request CAB-1147,",
                     "two approvals, and a risk assessment with a section titled 'Blast Radius'.");
            int c = UI.choose("The one-line fix:",
                "Follow the process. All of it.",
                "Backchannel it -- Marcus knows a guy who knows the deploy pipeline");
            if (c == 0) { p.culture += 8; UI.narrate("Three weeks later it ships. The form was the job. You're starting to see that."); }
            else { p.culture -= 5; p.relAdd("marcus", 2); UI.scene("Shipped by Friday.", "MARCUS: \"Didn't see anything. Nice fix.\""); }
        } else if (co.equals("startupx")) {
            UI.scene("THE PIVOT. Overnight, the product changes from B2B SaaS to",
                     "'AI-powered pet logistics'. Your sprint board is just... gone.",
                     "FOUNDER: \"Same mission, different vertical!\" It is not the same mission.");
            int c = UI.choose("The pivot:",
                "Roll with it -- ship the dog-routing MVP",
                "Push back in the all-hands -- someone has to say it");
            if (c == 0) { p.culture += 8; UI.narrate("The MVP ships. A golden retriever named Bus is your first user."); }
            else { p.culture -= 5; p.relAdd("founder", 2); UI.scene("A silence. Then the founder nods slowly.", "FOUNDER: \"Spicy. I like it. We'll circle back.\" They do not circle back.", "But he remembers you said it."); }
        } else {
            UI.scene("THE RETIREMENT. Deirdre -- the only person alive who understands the core system --",
                     "retires Friday. Her knowledge is in no wiki. It is in Deirdre.");
            int c = UI.choose("Your week:",
                "Shadow her. Write down everything. Everything.",
                "You have your own tickets. Someone else will do it.");
            if (c == 0) { addSkill("devops", 1); p.culture += 8; UI.scene("Forty pages of notes. The system survives her.", "DEIRDRE: \"You're the first one who asked.\""); }
            else { p.culture -= 8; p.set("deirdreLost", "true"); UI.narrate("Friday comes. Cake is eaten. Knowledge walks out the door in a sensible coat."); }
        }
    }

    static void b2_5() {
        UI.scene("Coffee with Marcus. He stirs it like the spoon owes him money.",
                 "MARCUS: \"Performance reviews are written in week three. Everything after",
                 "week three is theatre. You want the return offer? Make the theatre good.\"");
        p.set("reviewRevealed", "true");
        UI.info("  (Your running review score now shows in `status`.)");
    }

    static void b2_6() {
        UI.scene("DEMO DAY. You present what you built. Your voice does the wobble in the first",
                 "minute, then settles. Someone asks 'what about scale?' -- there is always one.");
        int bump = Math.min(10, p.portfolio / 5);
        p.set("demoBump", String.valueOf(bump));
        if (bump >= 6) UI.ok("  The demo lands. The portfolio work shows. (review +" + bump + ")");
        else UI.warn("  It's fine. Fine is a word people use. (review +" + bump + ")");
    }

    static int reviewScore() {
        int aoife = p.is("aoifeState", "ally") ? 15 : p.is("aoifeState", "gone") ? -15 : 0;
        return p.internWork + p.culture / 2 + aoife + p.flagInt("demoBump");
    }

    static void b2_7() {
        int score = reviewScore();
        UI.scene("THE REVIEW. A meeting room called 'Synergy'. Your manager, and HR Linda.",
                 "LINDA (reading the culture stat like a weather report): \"Mixed conditions,",
                 "some strong collaboration fronts, occasional overwork showers.\"");
        String outcome;
        if (score >= 65) {
            outcome = "return";
            p.addMoney(500);
            p.title = "One of the Good Ones";
            UI.scene("MANAGER: \"We'd like you back. Properly. There's a number in the envelope.\"",
                     "EUR +500. Title earned: One of the Good Ones.",
                     "The loaner laptop goes back. They let you keep the stickers.");
        } else if (score >= 40) {
            outcome = "neutral";
            UI.scene("MANAGER: \"Solid quarter. We're not hiring juniors right now, but -- good luck out there.\"",
                     "Handshakes. The loaner laptop goes back. A security guard you like waves you out.");
        } else {
            outcome = "pip";
            p.addMoney(-200);
            p.set("scarTissue1", "true");
            RPG.award("survived_pip", "Survived a PIP");
            UI.scene("The letters PIP are said out loud. Then, two weeks later, the other letters.",
                     "You hand over the loaner at the desk. The drawer closes with a very final click.",
                     "",
                     "Here's what nobody tells you about getting let go: the sky doesn't fall.",
                     "It just rains a normal amount. This is Dublin.",
                     "",
                     "Perk gained: Scar Tissue I (-10% burnout gain, permanent).");
        }
        p.set("reviewOutcome", outcome);
        p.loaner = false;

        // chapter 2 reflection
        List<String> r = new ArrayList<>();
        r.add("The internship year. " + coName() + ".");
        if (p.is("internCompany", "gigacorp"))     r.add("I learned what process is for, mostly by drowning in it.");
        if (p.is("internCompany", "startupx"))     r.add("I learned what no process costs, mostly at 4:50pm on Fridays.");
        if (p.is("internCompany", "publicsector")) r.add("I learned that boring systems hold the country up. Somebody has to mind them.");
        if (p.is("aoifeState", "ally"))  r.add("Aoife's referral got my CV to the top of a pile a month later. People remember.");
        if (p.is("aoifeState", "gone"))  r.add("I still think about the morning I pretended not to see her.");
        if (outcome.equals("return"))    r.add("They wanted me back. You'd be amazed what that does for the spine.");
        if (outcome.equals("pip"))       r.add("Getting let go was the worst thing that happened to me that year. It wasn't even close to the worst thing that happened to anyone.");
        UI.card("", r.toArray(new String[0]));
    }

    // ---------------------------------------------------------------- chapter 3

    static void ch3open() {
        UI.card("CHAPTER THREE -- JUNIOR",
            "A real job. A real salary. And a real feeling, every single morning,",
            "that today is the day they find out you've been guessing.",
            "(Everyone is guessing. Nobody tells you that for years.)");
        String outcome = p.flag("reviewOutcome");
        p.imposter = outcome.equals("return") ? 55 : outcome.equals("pip") ? 85 : 70;
        if (outcome.equals("return")) { p.addMoney(300); UI.info("  Signing bonus: EUR +300. Being wanted travels with you."); }
        UI.info("  Imposter meter active: " + p.imposter + "/100. Shipping things lowers it.");
    }

    static void b3_1() {
        UI.scene("Week one. You meet DARRAGH (tech lead, reads diffs like sheet music)",
                 "and NIAMH (the other junior, started Monday, already knows where the biscuits live).",
                 "Wednesday: you push a config change. Production goes down for eleven minutes.",
                 "You watch the graphs fall and feel your soul leave through your shoes.",
                 "DARRAGH: \"Good. Now you're one of us. The ones who haven't broken prod yet",
                 "just haven't deployed yet.\"");
        if (p.is("reviewOutcome", "pip"))
            UI.scene("DARRAGH (later, quietly): \"I read your file. PIPs are where the interesting ones come from.\"");
    }

    static void b3_2() {
        UI.scene("You and Niamh pair until 4am on a race condition that only happens on Thursdays.",
                 "At standup, fresh as a daisy, she presents the fix. Singular. Hers.");
        int c = UI.choose("You:",
            "Let it go. Not worth it.",
            "Talk to her privately afterwards",
            "Correct her. In the meeting. Now.");
        if (c == 0) {
            p.set("niamhState", "neutral");
            UI.narrate("You let it go. It doesn't quite let go of you.");
        } else if (c == 1) {
            p.set("niamhState", "friend");
            UI.scene("She goes white, then red.",
                     "NIAMH: \"Oh god. I didn't even -- I was so tired I genuinely thought--\"",
                     "Next standup she corrects the record, unprompted, in front of everyone.",
                     "You will never have a more loyal friend in this industry.");
        } else {
            p.set("niamhState", "rival");
            UI.scene("You say it evenly. The room goes quiet. You get the credit.",
                     "I was right. Being right cost more than I thought it would.");
        }
    }

    static void b3_3() {
        UI.card("TUESDAY, 2:14 AM",
            "The phone. The bad noise. The checkout is down,",
            "the runbook is wrong, and the senior on backup isn't answering.");
        UI.scene("You are alone with it. Money is not moving. Every minute is a number.");
        UI.choose("First move:",
            "Read the logs. Actually read them.",
            "Restart everything and pray");
        UI.scene("There. 2:09am: a dependency rolled out a 'minor' version. Minor. Sure.");
        int c2 = UI.choose("Call it:",
            "Roll back to the last good version",
            "Hotfix forward -- pin the dependency");
        UI.scene(c2 == 0 ? "Rollback. Four minutes. The graphs breathe again."
                         : "Pin, build, ship. Seven minutes, hands steady-ish. Graphs recover.",
                 "You write the incident summary at 3am: what broke, what fixed it, no blame.",
                 "The senior on backup replies at 8:15: 'great handling'. He was asleep. You weren't.");
        p.reputation += 2;
        p.imposterShip(15);
        p.addMoney(80);
        RPG.award("two_fourteen_am", "2:14 AM");
        UI.narrate("Nothing good has ever happened on a Tuesday.");
    }

    static void b3_4() {
        UI.scene("Pints. Ciaran's buying, which means something's up. His startup is real now: SELKIE.",
                 "CIARAN: \"It's Stripe for invoices.\"",
                 "YOU: \"Stripe is Stripe for invoices.\"",
                 "CIARAN: \"It's Stripe for *Irish* invoices. The Revenue integration alone --",
                 "look, do you want in or not?\"");
        if (p.relOf("ciaran") < 0)
            UI.scene("CIARAN: \"I know, I know -- 'you'll regret it.' Say it again, I'll wait.\"");
        if (p.rideTier >= 2)
            UI.narrate("He pitched me a startup while I drank a Lucozade. I respected the commitment.");
        if (p.laptopTier < 2 && p.level >= 25)
            UI.scene("CIARAN: \"You're not planning to clone Selkie onto a company laptop.",
                     "Tell me that's not the plan.\"");
        UI.say("  The offer: founding engineer. Nights and weekends. 4% equity. No pay.");
        int c = UI.choose("Selkie:",
            "In. (`sideproject` becomes `startupwork`: double stamina, real stakes)",
            "Out. The day job is enough job.");
        if (c == 0) {
            p.set("joinedSelkie", "true");
            UI.scene("CIARAN: \"Knew it. Repo invite's already sent. It was sent yesterday.\"");
        } else {
            p.set("joinedSelkie", "false");
            UI.scene("CIARAN: \"Grand. Had to ask. You were the first call, for what it's worth.\"",
                     "He means it, which somehow makes it worse.");
        }
    }

    static void b3_5() {
        UI.scene("A conference CFP is open. Niamh forwards it with the subject line 'do it, coward'.");
        int c = UI.choose("Submit a talk?",
            "Submit -- \"" + Data.talkTitle(p.flag("gradPath")) + "\"",
            "Not for you. Stages are for other people.");
        if (c == 1) {
            p.set("talkDelivered", "false");
            UI.narrate("The CFP closes quietly a month later. Doors do that.");
            return;
        }
        UI.scene("Six weeks later: \"We received a record number of submissions this year...\"",
                 "Rejected. Everyone's rejection email. There is a numb afternoon.");
        int c2 = UI.choose("Next year's CFP opens:",
            "Resubmit. Tighter abstract. Angrier title.",
            "Once was enough.");
        if (c2 == 1) {
            p.set("talkDelivered", "false");
            UI.narrate("Some doors you close yourself, politely, from the inside.");
            return;
        }
        UI.scene("ACCEPTED.",
                 "You deliver \"" + Data.talkTitle(p.flag("gradPath")) + "\" to a room of two hundred people.",
                 "Your slides work. Your demo works. Your voice only does the wobble once.",
                 "",
                 "Afterwards, one email. FROM: Brennan.",
                 "\"Saw the recording. The loop invariants were sloppy. Good talk.\"");
        p.set("talkDelivered", "true");
        p.reputation += 3;
        p.imposterShip(10);
    }

    static void b3_6() {
        int score = p.flagInt("selkieScore");
        boolean funded = score >= 6;
        if (funded) {
            p.set("selkieOutcome", "funded");
            p.addMoney(2000);
            UI.scene("SELKIE IS FUNDED. A real seed round, a real office, real staff.",
                     "Your nights are your own again. The advisory shares stay.",
                     "CIARAN: \"Told you. *Irish* invoices.\" EUR +2000.");
        } else {
            p.set("selkieOutcome", "folded");
            p.set("scarTissue2", "true");
            UI.scene("Selkie folds. Eighteen months. One county council pilot.",
                     "CIARAN: \"Grand. ...Next one's the one.\"",
                     "The strange thing is, he meant it. The stranger thing is, I believed him.",
                     "",
                     "Perk gained: Scar Tissue II (further burnout resistance).");
        }
    }

    static void b3_7() {
        UI.scene("Darragh, coffee, no preamble.",
                 "DARRAGH: \"Two ways up from here, and they're not the same job.",
                 "Staff engineer: you go deep -- the hard problems, the architecture, the code.",
                 "Lead: you go wide -- the people, the plans, the meetings about meetings.",
                 "Both are real work. Don't let anyone tell you otherwise.",
                 "But pick the one you can stomach on a bad Tuesday.\"");
        int c = UI.choose("The Fork:",
            "Staff Engineer -- go deep (IC track)",
            "Team Lead -- go wide (Management track)");
        p.set("seniorTrack", c == 0 ? "ic" : "mgmt");

        // chapter 3 reflection
        List<String> r = new ArrayList<>();
        r.add("The junior years. The long middle.");
        if (p.is("niamhState", "friend"))  r.add("Niamh and I still argue about that race condition. Friends do that.");
        if (p.is("niamhState", "rival"))   r.add("Niamh and I were polite for years after the standup. Polite is its own punishment.");
        if (p.isTrue("joinedSelkie"))      r.add("The Selkie nights cost me sleep I'm still owed. Worth it. Probably.");
        if (p.is("talkDelivered", "true")) r.add("The talk changed things. Not the talk itself -- the fact of having done it.");
        UI.card("", r.toArray(new String[0]));
    }

    // ---------------------------------------------------------------- chapter 4 -- IC track

    static void ch4open() {
        if (p.is("seniorTrack", "mgmt"))
            UI.card("CHAPTER FOUR -- LEAD",
                "You have reports now.",
                "They look at you the way you used to look at Darragh.",
                "God help them.");
        else
            UI.card("CHAPTER FOUR -- STAFF",
                "You barely write code anymore. You write the documents that decide the code.",
                "It took ten years to learn the job. Then the job changed.");
    }

    static void i4_1() {
        UI.scene("THE DOCUMENT. The monolith has to go -- everyone's known for two years,",
                 "and now it's yours. You open a blank doc titled 'RFC: Migration'.",
                 "Six weeks of drafts. Diagrams. A glossary. A section called 'What We Are Not Doing',",
                 "which is the most important section and the one nobody reads.");
        p.reputation += 1;
        UI.info("  +1 reputation. The document exists. That's most of the battle.");
    }

    static void i4_2() {
        UI.scene("A new senior joins. It's Marcus. (\"Village,\" he says, before you can.)",
                 "He reviews the RFC. Six comments. All of them are correct.",
                 "All of them are load-bearing. Two of them hurt.",
                 "MARCUS: \"This is the most useful thing I'll do for you all year.\"");
        addSkill("basics", 1);
        p.reputation += 1;
        UI.info("  You revise. The document gets better. So do you.");
    }

    static void i4_3() {
        int c = UI.choose("The migration, for real now:",
            "Big bang -- one weekend, all of it, hold your breath",
            "Strangler fig -- route by route, months of careful");
        if (c == 0) {
            if (RNG.nextInt(100) < 40) {
                UI.scene("Saturday 11pm: the session store doesn't migrate. Forty minutes of darkness.",
                         "You roll forward -- the old system is already gone. It holds. Barely.",
                         "Monday's incident review is... thorough. But it's done. It's actually done.");
            } else {
                UI.scene("It works. One weekend. The kind of clean cutover people write conference talks about.",
                         "You know exactly how lucky you got. That knowledge is the real senior skill.");
            }
        } else {
            UI.scene("Route by route, month by month. No drama. No incident. No glory.",
                     "The old monolith serves its last request on a Wednesday and nobody notices.",
                     "That was the point. Nobody noticing was the whole point.");
        }
        p.reputation += 2;
        RPG.gainXp(40);
    }

    static void i4_4() {
        UI.scene("Your mentee breaks prod in week one. A config change. The graphs fall.",
                 "They look at you the way you once looked at the floor, wishing it would open.",
                 "You hear yourself say:",
                 "\"Good. Now you're one of us.\"",
                 "",
                 "The words weren't mine. They were Darragh's. That's how this works --",
                 "nothing is yours, you just mind it for a while.");
        p.imposterShip(10);
    }

    static void i4_5() {
        boolean open = p.is("talkDelivered", "true") || p.is("siobhanState", "ally");
        if (open) {
            UI.scene("A calendar invite from a name you haven't seen in years: Siobhan.",
                     "She's a principal at a US giant now. Coffee, the good place, her shout.",
                     p.is("talkDelivered", "true")
                         ? "SIOBHAN: \"I saw the talk. I'm not headhunting. I'm saying the door's open."
                         : "SIOBHAN: \"I've known since the hackathon. I'm not headhunting. The door's open.",
                     "I know what you're like about open doors.\"");
            p.set("bigTechUnlocked", "true");
        } else {
            UI.scene("You hear it secondhand: Siobhan was in town. Recruiting. Senior roles, US money.",
                     "The coffee never happens. You were never on the list.",
                     "Doors you never knew were there are still doors.");
        }
    }

    // ---------------------------------------------------------------- chapter 4 -- management track

    static void m4_1() {
        UI.scene("THE REQ. Your first hire. Two finalists.",
                 "One: solid CV, right stack, sensible answers. You know exactly what you'd get.",
                 "Two: weird CV, solved the take-home in a language your team doesn't use,",
                 "and an answer to 'why here?' that you keep thinking about.");
        int c = UI.choose("Offer goes to:",
            "The safe candidate",
            "The wildcard");
        p.set("hireChoice", c == 0 ? "safe" : "wildcard");
        UI.narrate(c == 0 ? "Sensible. Teams are built on sensible." : "Interesting. Teams are changed by interesting.");
    }

    static void m4_2() {
        if (p.is("niamhState", "friend")) {
            UI.scene("Reorg. Niamh lands on your team -- now your report.",
                     "NIAMH: \"To be clear, I remember when you broke prod in week one.\"",
                     "YOU: \"To be clear, that's insubordination.\"",
                     "Best working relationship of your career. Nothing needs to be said. It was said years ago.");
        } else if (p.is("niamhState", "rival")) {
            UI.scene("Reorg. Niamh lands on your team -- now your report.",
                     "Within a week, a transfer request is sitting in the system. You know why.");
            int c = UI.choose("The standup. Years ago. You got the credit.",
                "Address it. Out loud. Properly.",
                "Let the transfer go through.");
            if (c == 0) {
                p.set("niamhState", "friend");
                UI.scene("YOU: \"I was right. It wasn't worth it.\"",
                         "A long pause.",
                         "NIAMH: \"...No. It wasn't.\" The transfer request quietly disappears.");
            } else {
                UI.narrate("She transfers. Good engineer. The history goes with her.");
            }
        } else {
            UI.scene("Reorg. Niamh lands on your team -- now your report.",
                     "You were never close. You're not close now. But she's good, and you say so",
                     "in writing, where it counts. That's the job.");
        }
    }

    static void m4_3() {
        UI.card("A TUESDAY",
            "VP Siobhan sends a spreadsheet. Cost reductions.",
            "One name on it is yours to cut.");
        List<String> opts = new ArrayList<>();
        opts.add("Fight it -- escalate, push back, spend your capital");
        opts.add("Comply -- pick the name, make the call");
        boolean canBudget = p.money >= 500;
        if (canBudget) opts.add("Offer budget cuts instead -- EUR 500 of your own discretionary spend");
        int c = UI.choose("The spreadsheet:", opts.toArray(new String[0]));
        if (c == 0) {
            p.set("spreadsheetChoice", "fought");
            UI.scene("You fight it. Two meetings, one email you draft eleven times.",
                     "The name comes off the list. Someone else's list, someone else's name -- you know that.",
                     "SIOBHAN: \"Noted.\" It costs you with her. The team never knows what didn't happen.");
        } else if (c == 1) {
            p.set("spreadsheetChoice", "complied");
            UI.scene("You make the call on a Tuesday afternoon. You do it properly, kindly, face to face.",
                     "It is the worst thing the job has ever asked of you. You do it anyway.",
                     "SIOBHAN: \"Handled well.\" The words sit wrong for weeks.");
        } else {
            p.set("spreadsheetChoice", "budget");
            p.addMoney(-500);
            UI.scene("You go line by line: conference budget, tooling, the team offsite. EUR -500.",
                     "Everyone stays. Nobody knows what the offsite was traded for.",
                     "Nothing good has ever happened on a Tuesday.");
        }
    }

    static void m4_4() {
        if (p.is("hireChoice", "safe")) {
            UI.scene("The safe hire, six months in: quietly carries the on-call rotation.",
                     "Never brilliant, never absent. The team's resting heart rate is lower with them on it.",
                     "Not every win shows up on a graph. Most don't.");
        } else if (RNG.nextBoolean()) {
            UI.scene("The wildcard, six months in: ships an internal tool nobody asked for.",
                     "Within a month the whole org uses it daily. The VP asks who hired them.",
                     "You say you did. You leave out how close a call it was.");
        } else {
            UI.scene("The wildcard, six months in: causes the single most magnificent incident",
                     "of your career -- a migration script with a hand-rolled YAML parser.",
                     "The post-mortem is forty pages. They write it themselves, brilliantly. You keep them.",
                     "Wins come with asterisks. You're learning to read the footnotes.");
        }
    }

    static void m4_5() {
        boolean open = p.is("talkDelivered", "true") || p.is("siobhanState", "ally")
                    || p.is("spreadsheetChoice", "fought");
        if (open) {
            UI.scene("Skip-level with VP Siobhan. Exit interview energy. She's leaving for Seattle.",
                     p.is("spreadsheetChoice", "fought")
                         ? "SIOBHAN: \"You fought me on the spreadsheet. Cost you, and you knew it would."
                         : "SIOBHAN: \"You didn't fight me on the spreadsheet. But I've watched you work.",
                     "Either way -- there's a role going in Seattle that needs someone who can survive me.\"");
            p.set("bigTechUnlocked", "true");
        } else {
            UI.scene("Skip-level with VP Siobhan. She's leaving for Seattle. The role she's filling",
                     "behind her goes to someone you've never heard of.",
                     "SIOBHAN (at the door): \"There was a version of this where I asked you. For what it's worth.\"",
                     "Doors you never knew were there are still doors.");
        }
    }

    // ---------------------------------------------------------------- finale -- Three Doors (§9)

    static void finale() {
        // resolve Selkie if the player never joined
        if (p.flag("selkieOutcome").isEmpty()) {
            p.set("selkieOutcome", RNG.nextBoolean() ? "funded" : "folded");
            UI.narrate("Word reaches you about Selkie, the thing Ciaran built without you: it " +
                (p.is("selkieOutcome", "funded") ? "got funded. A real round. Fair play." : "folded last spring. He's already building the next one."));
        }
        // chapter 4 reflection (management quotes the spreadsheet)
        List<String> r = new ArrayList<>();
        r.add("Ten years.");
        if (p.is("seniorTrack", "mgmt")) {
            if (p.is("spreadsheetChoice", "fought"))   r.add("\"Fight it.\" I'd say it again. It cost what it cost.");
            if (p.is("spreadsheetChoice", "complied")) r.add("\"Handled well.\" Some compliments you carry like stones.");
            if (p.is("spreadsheetChoice", "budget"))   r.add("Nobody ever knew about the offsite. That was the deal I made with myself.");
        } else {
            r.add("The monolith is gone. The document that killed it has my name on it.");
        }
        UI.card("", r.toArray(new String[0]));

        doors();
    }

    /** The Three Doors. Re-enterable via the `doors` command until an ending is chosen. */
    static void doors() {
        if (!p.flag("ending").isEmpty()) { UI.dim("  The story has been told. (prestige to tell it again)"); return; }
        UI.card("THREE DOORS",
            "Level 50. The question arrives, the way it always does,",
            "casually, over nothing: what now?");
        boolean bigtech = p.isTrue("bigTechUnlocked");
        boolean indie = p.money >= 5000 || p.is("selkieOutcome", "funded");
        List<String> opts = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        opts.add("STAY -- the work, the team, the place that knows you"); keys.add("stay");
        if (bigtech) { opts.add("BIG TECH -- Siobhan's door. Seattle. The enormous number."); keys.add("bigtech"); }
        else UI.dim("  [LOCKED] BIG TECH -- " + (p.is("talkDelivered", "true") ? "Siobhan never made the offer." : "You never gave the talk. Nobody in Seattle knows your name."));
        if (indie) { opts.add("GO INDIE -- found the thing. Burn the boats."); keys.add("indie"); }
        else UI.dim("  [LOCKED] GO INDIE -- founding takes runway (EUR 5,000) or a funded friend. You have EUR " + p.money + ".");
        int c = UI.choose("Three doors. " + (keys.size() < 3 ? "Well. " + keys.size() + "." : ""), opts.toArray(new String[0]));
        switch (keys.get(c)) {
            case "stay":    endingStay(); break;
            case "bigtech": endingBigTech(); break;
            case "indie":   endingIndie(); break;
        }
    }

    static void endingStay() {
        p.set("ending", "stay");
        UI.card("DOOR ONE -- STAY",
            "Friday. A new grad on your team, end of their first week,",
            "hovers at your desk with two coffees. A bribe. A good one.",
            "",
            "NEW GRAD: \"Can I ask -- how did you get into tech?\"",
            "",
            "So I told them. The long version. You've just heard it.");
        UI.scene("THE END.",
                 "(The whole game was this answer. `prestige` to tell it again --",
                 "stories improve in the telling. +5% Embellishment per telling.)");
    }

    static void endingBigTech() {
        UI.scene("THE GAUNTLET. Five stages. Everyone warned you. They undersold it.",
                 "STAGE 1 -- recruiter screen. 'Walk me through your background.' You walk her through it.",
                 "She says 'awesome' eleven times. You advance.");
        UI.scene("STAGE 2 -- the algorithms round.");
        UI.narrate("Brennan's voice in your head: read the loop before you answer.");
        Data.Question[] bank = Data.COURSES.get("algorithms").bank;
        List<Data.Question> qs = new ArrayList<>(List.of(bank));
        Collections.shuffle(qs, RNG);
        int right = 0;
        for (int i = 0; i < 3; i++) if (Events.ask(qs.get(i))) right++;
        if (right < 2) {
            UI.scene("\"We've decided not to move forward at this time. You're welcome to reapply.\"",
                     "The door stays where it is. Doors do. (`doors` to try again.)");
            return;
        }
        UI.scene("Somewhere in Dublin, a professor felt a disturbance.",
                 "STAGE 3 -- system design. You draw boxes. You draw arrows between the boxes.",
                 "You say 'it depends' with the confidence of someone who knows what it depends on.",
                 "STAGE 4 -- behavioural. The interviewer's name badge says LINDA.",
                 "YOU: \"...Linda?\"",
                 "LINDA: \"There's a Linda everywhere.\"",
                 "STAGE 5 -- the offer. You read the number twice. Then once more, slower.");
        p.set("ending", "bigtech");
        p.addMoney(20000);
        p.title = "Big Tech";
        UI.card("DOOR TWO -- BIG TECH",
            "A glass office with your name by the door. EUR +20,000. 11pm.",
            "",
            "I got everything I asked for. Most days, that feels like enough.",
            "That's the honest version. Most days.");
        UI.scene("THE END.", "(`prestige` to tell the story again.)");
    }

    static void endingIndie() {
        String selkie = p.flag("selkieOutcome");
        if (selkie.equals("funded") && p.isTrue("joinedSelkie")) {
            UI.scene("CIARAN: \"Last time, I asked you over pints and you said no-- alright, *nights only*.",
                     "This time I'm asking properly. CTO. Come build the thing.\"");
        } else if (selkie.equals("folded") && p.isTrue("joinedSelkie")) {
            UI.scene("Round two. Together this time, from day one.",
                     "CIARAN: \"Next one's the one. I told you I meant it.\"");
        } else {
            UI.scene("You go alone. The morning you resign, one text arrives. Ciaran:",
                     "\"welcome to the queue-skippers. it's terrible here. you'll love it.\"");
        }
        int invest = 0;
        if (p.money >= 3000) {
            int c = UI.choose("Runway:", "Invest EUR 3,000 of savings -- longer runway, better odds", "Bootstrap on fumes");
            if (c == 0) { invest = 15; p.addMoney(-3000); }
        }
        int chance = Math.min(90, 35 + p.portfolio / 5 + p.reputation * 2
                + (selkie.equals("funded") ? 20 : 0) + invest);
        UI.dim("  (Founding odds: " + chance + "%. Portfolio, reputation and runway all counted.)");
        if (RNG.nextInt(100) < chance) {
            p.set("ending", "indie_success");
            p.title = "Founder";
            UI.card("DOOR THREE -- GO INDIE",
                "Fourteen months later: your own company's first all-hands.",
                "Eleven people. A rented room over a pub.",
                "The best office I ever had.");
            UI.scene("THE END.", "(`prestige` to tell the story again.)");
        } else {
            p.set("ending", "indie_fail");
            p.set("founderScar", "true");
            UI.card("DOOR THREE -- GO INDIE",
                "It lasted fourteen months.",
                "I'd do it again.",
                "In fact --");
            UI.scene("ATTEMPT #2 STARTS MONDAY.",
                     "Perk gained: Founder Scar Tissue (+10% money, permanent, survives retellings).",
                     "THE END. (`prestige` to tell the story again.)");
        }
    }
}
