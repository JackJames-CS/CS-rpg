import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Static game data: jobs, shop, laptop/transport lines, courses + question banks, quests. */
class Data {

    // ---------------------------------------------------------------- jobs

    static class Job {
        final String name, phase, tag;
        final int pay, xp;
        final boolean needsLaptop; // needs the ThinkPad or better (coding job)
        Job(String name, String phase, int pay, int xp, String tag, boolean needsLaptop) {
            this.name = name; this.phase = phase; this.pay = pay; this.xp = xp;
            this.tag = tag; this.needsLaptop = needsLaptop;
        }
    }

    static final Job[] JOBS = {
        new Job("Tutor",                     "STUDENT", 20, 10, "basics",   false),
        new Job("Bug Fixer",                 "STUDENT", 20, 12, "basics",   true),
        new Job("App Tester",                "STUDENT", 15,  9, "basics",   false),
        new Job("Research Assistant",        "STUDENT", 22, 11, "ml",       true),
        new Job("Open Source Contributor",   "STUDENT", 18, 13, "backend",  true),
        new Job("Intern Developer",          "INTERN",  75, 16, "basics",   true),
        new Job("QA Tester",                 "INTERN",  60, 14, "basics",   false),
        new Job("IT Support",                "INTERN",  55, 12, "devops",   false),
        new Job("Documentation Writer",      "INTERN",  58, 13, "basics",   false),
        new Job("Platform Support",          "INTERN",  62, 14, "devops",   false),
        new Job("Junior Developer",          "JUNIOR", 120, 20, "basics",   true),
        new Job("Web Developer",             "JUNIOR", 110, 19, "frontend", true),
        new Job("App Developer",             "JUNIOR", 130, 21, "frontend", true),
        new Job("API Developer",             "JUNIOR", 125, 21, "backend",  true),
        new Job("Test Automation Engineer",  "JUNIOR", 115, 19, "devops",   true),
        new Job("Backend Engineer",          "SENIOR", 220, 28, "backend",  true),
        new Job("Frontend Engineer",         "SENIOR", 210, 28, "frontend", true),
        new Job("AI Engineer",               "SENIOR", 250, 30, "ml",       true),
        new Job("DevOps Specialist",         "SENIOR", 230, 29, "devops",   true),
        new Job("Site Reliability Engineer", "SENIOR", 235, 29, "devops",   true),
        new Job("Security Engineer",         "SENIOR", 240, 29, "devops",   true),
    };

    static int phaseRank(String phase) {
        switch (phase) {
            case "STUDENT": return 0;
            case "INTERN":  return 1;
            case "JUNIOR":  return 2;
            default:        return 3;
        }
    }

    // ---------------------------------------------------------------- laptop line (§12.1)

    static final String[]  LAPTOP_NAMES = { "The Hairdryer", "Second-hand ThinkPad", "Your Own Machine", "The Dream Machine" };
    static final int[]     LAPTOP_COST  = { 0, 200, 1200, 2800 };
    static final int[]     LAPTOP_BONUS = { 0, 8, 25, 40 };          // EUR per work
    static final int[]     LAPTOP_MINLV = { 1, 1, 25, 41 };
    static final String[]  LAPTOP_COPY  = {
        "Nobody would buy it. You've tried.",
        "Built like a tank. Previously owned by one, going by the dents.",
        "The first machine you didn't have to apologise for.",
        "Two weeks on the spec sheet. Three days to ship. The guilt: ongoing.",
    };

    // ---------------------------------------------------------------- transport line (§12.2)

    static final String[] RIDE_NAMES  = { "Shoe leather", "Second-hand Bike", "The Starter Car", "The Reliable Car" };
    static final int[]    RIDE_COST   = { 0, 120, 1500, 4000 };
    static final int[]    RIDE_BURN   = { 0, 1, 2, 3 };              // burnout reduction on work
    static final int[]    RIDE_MINLV  = { 1, 1, 25, 41 };
    static final String[] RIDE_COPY   = {
        "Free, like all the worst things.",
        "You arrive damp but on time. The damp is permanent. So is the on-time.",
        "An '09 Corolla, 168k km, one wing mirror held on by hope.",
        "It just passes. That's what the money was for.",
    };
    static final String[] RIDE_BUYLINE = {
        "",
        "The rain remains undefeated, but at least now it's a fair fight.",
        "The ball joint. It's always the ball joint.",
        "Adulthood is when reliability becomes a luxury good.",
    };

    // ---------------------------------------------------------------- shop items (§12.3)

    static class Item {
        final String key, name, desc, copy;
        final int cost, minLevel;
        final String requires;   // item key prerequisite, or null
        final boolean consumable;
        Item(String key, String name, int cost, int minLevel, String requires,
             boolean consumable, String desc, String copy) {
            this.key = key; this.name = name; this.cost = cost; this.minLevel = minLevel;
            this.requires = requires; this.consumable = consumable; this.desc = desc; this.copy = copy;
        }
    }

    static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    static {
        put(new Item("coffee", "Coffee", 5, 1, null, true,
            "Consumable: stamina +20, burnout -8.", "Dublin runs on it. So will you."));
        put(new Item("energydrink", "Energy Drink", 10, 1, null, true,
            "Consumable: stamina +40, burnout +8.", "Borrowing energy from a future you who will want it back."));
        put(new Item("deskplant", "Desk Plant", 15, 1, null, false,
            "Rest recovers +5 more stamina.", "It's doing its best."));
        put(new Item("raspberrypi", "Raspberry Pi", 80, 1, null, false,
            "+5 defence in hacker events.", "A paperweight with potential. Like everyone in first year."));
        put(new Item("keyboard", "Mechanical Keyboard", 120, 1, null, false,
            "+EUR 15 per work.", "Your flatmate can hear it through two walls. This is a feature."));
        put(new Item("onlinecourse", "Online Course", 200, 1, null, true,
            "Consumable: +5 skill points.", "Module 1 is free. Module 1 is always free."));
        put(new Item("headphones", "Noise-Cancelling Headphones", 250, 5, null, false,
            "+25% XP on study and attend.", "The library has a guy who chews. Every library has a guy who chews."));
        put(new Item("tower", "The Tower", 450, 5, null, false,
            "Second-hand desktop for the flat: project gains +1 portfolio.",
            "Three previous owners. One of them mined crypto on it. It runs hot, and ashamed."));
        put(new Item("dualmonitors", "Dual Monitors", 350, 5, "tower", false,
            "Project portfolio gains x1.5. Needs the Tower.", "One for the code. One for pretending to read documentation."));
        put(new Item("standingdesk", "Standing Desk", 500, 25, null, false,
            "Burnout per action +3 instead of +6.", "You will stand for exactly nine days, then use it as a shelf. The shelf helps too."));
    }
    static void put(Item i) { ITEMS.put(i.key, i); }

    // ---------------------------------------------------------------- courses (§3)

    static class Question {
        final String prompt, answer, wrong1, wrong2, resultLine;
        Question(String prompt, String answer, String wrong1, String wrong2, String resultLine) {
            this.prompt = prompt; this.answer = answer; this.wrong1 = wrong1; this.wrong2 = wrong2;
            this.resultLine = resultLine;
        }
    }

    static class Course {
        final String key, name, skill;
        final int year, skillDistinction; // skill levels granted: distinction = this, merit = half (min 1)
        final String[] prereqs;
        final Question[] bank;
        Course(String key, String name, int year, String[] prereqs, String skill,
               int skillDistinction, Question[] bank) {
            this.key = key; this.name = name; this.year = year; this.prereqs = prereqs;
            this.skill = skill; this.skillDistinction = skillDistinction; this.bank = bank;
        }
    }

    static final Map<String, Course> COURSES = new LinkedHashMap<>();
    static {
        course(new Course("cs101", "CS101: Intro to Programming", 1, new String[]{}, "basics", 2, new Question[]{
            new Question("int x = 0; for (int i = 0; i < 5; i++) x += i;  -- final value of x?",
                "10", "15", "5",
                "0+1+2+3+4. The off-by-one gods spare you. Today."),
            new Question("What does the expression i++ evaluate to?",
                "the old value of i", "the new value of i", "undefined",
                "Increment after. It's in the name, eventually."),
            new Question("In Java, \"5\" + 3 is?",
                "\"53\"", "8", "compile error",
                "Strings eat numbers. Always have."),
        }));
        course(new Course("webfund", "Web Fundamentals", 1, new String[]{"cs101"}, "frontend", 1, new Question[]{
            new Question("Which of these runs on the server?",
                "Node.js", "CSS", "browser JS",
                "The browser is not your friend. It's your audience."),
            new Question("The thing exists but you're not allowed: which status code?",
                "403", "404", "500",
                "404 is 'never heard of it.' 403 is 'I know exactly what you want. No.'"),
            new Question("In JS, \"5\" == 5 is?",
                "true", "false", "TypeError",
                "Coercion. This is why === exists. Use it and tell no one you ever didn't."),
        }));
        course(new Course("datastructures", "Data Structures", 2, new String[]{"cs101"}, "backend", 1, new Question[]{
            new Question("O(1) lookup by key -- which structure?",
                "hash map", "sorted array", "linked list",
                "Hash it and forget it."),
            new Question("You're building undo. Which structure?",
                "stack", "queue", "heap",
                "Last in, first regretted."),
            new Question("BFS uses which structure for the frontier?",
                "queue", "stack", "priority heap",
                "Stack gives you DFS and a lecture from Brennan."),
        }));
        course(new Course("networks", "Computer Networks", 2, new String[]{"cs101"}, "devops", 1, new Question[]{
            new Question("curl https://... hangs, then 'connection refused' instantly on retry -- most likely?",
                "nothing listening on the port", "DNS failure", "certificate expired",
                "The port never opened. It happens to everyone. Usually in a demo."),
            new Question("HTTPS default port?",
                "443", "80", "8080",
                "80 is the past. 8080 is a cry for help."),
            new Question("Video call -- TCP or UDP?",
                "UDP", "TCP", "ICMP",
                "Late audio is worse than lost audio. Drop the packet, keep the vibe."),
        }));
        course(new Course("databases", "Databases", 2, new String[]{"datastructures"}, "backend", 1, new Question[]{
            new Question("SELECT * FROM users WHERE name = 'O'Brien' -- what happens?",
                "syntax error: unescaped quote", "returns O'Brien", "returns nothing",
                "Ireland: stress-testing string handling since the dawn of SQL."),
            new Question("An index speeds up reads. The price?",
                "slower writes + storage", "nothing, it's free", "slower reads too",
                "Nothing is free. Especially not in a database."),
            new Question("A list page fires one query per row. The name of this crime?",
                "N+1 problem", "deadlock", "cartesian join",
                "One join. You were one join away."),
        }));
        course(new Course("algorithms", "Algorithms", 3, new String[]{"datastructures"}, "basics", 1, new Question[]{
            new Question("for i in 0..n { for j in 0..i { ... } } -- complexity?",
                "O(n^2)", "O(n log n)", "O(n)",
                "The inner loop grows. Brennan saw you hesitate."),
            new Question("Binary search requires the input to be?",
                "sorted", "unique", "a power of two",
                "O(log n) or O(wrong), depending."),
            new Question("while (n > 1) n = n / 2;  -- complexity?",
                "O(log n)", "O(n)", "O(1)",
                "Halving is the whole trick. There is only one trick."),
        }));
        course(new Course("os", "Operating Systems", 3, new String[]{"datastructures"}, "devops", 1, new Question[]{
            new Question("while (running) { buf = malloc(64); use(buf); } -- what's wrong?",
                "memory leak, no free", "stack overflow", "dangling pointer",
                "It works. Briefly. Like all the best mistakes."),
            new Question("Two threads run counter++ with no lock -- result?",
                "race condition, lost updates", "deadlock", "works fine, it's atomic",
                "++ is three operations in a trenchcoat."),
            new Question("Threads of one process share...?",
                "the address space", "their stacks", "nothing",
                "Same house, separate diaries."),
        }));
        course(new Course("security", "Security Fundamentals", 3, new String[]{"networks"}, "devops", 1, new Question[]{
            new Question("query = \"SELECT * FROM users WHERE id = \" + userInput  -- the vulnerability?",
                "SQL injection", "XSS", "CSRF",
                "Little Bobby Tables sends his regards."),
            new Question("Store passwords as...?",
                "salted hashes", "encrypted", "plaintext but the DB is private",
                "'The DB is private' is the first line of every breach post-mortem."),
            new Question("The S in HTTPS protects you from...?",
                "eavesdroppers on the wire", "the server seeing your data", "viruses",
                "The server still sees everything. It's the lads in the middle who don't."),
        }));
        course(new Course("softeng", "Software Engineering", 4, new String[]{"algorithms"}, "basics", 1, new Question[]{
            new Question("Code review: a 400-line function named handleStuff(). The smell?",
                "does too many things -- split it", "needs more comments", "rename it",
                "Renaming it handleStuffProperly() is not refactoring."),
            new Question("Tests pass locally, fail in CI. First suspect?",
                "environment/config difference", "CI is broken", "cosmic rays",
                "'Works on my machine.' Grand. We'll ship your machine."),
            new Question("Which one rewrites history?",
                "rebase", "merge", "cherry-pick",
                "Rewriting history is fine until someone else was relying on it. Like all history."),
        }));
        course(new Course("statsml", "Statistics & ML", 4, new String[]{"algorithms"}, "ml", 1, new Question[]{
            new Question("99% train accuracy, 60% test accuracy -- diagnosis?",
                "overfitting", "underfitting", "great model, ship it",
                "It memorised the homework. So did half your class."),
            new Question("Fair coin, five heads in a row. P(heads next)?",
                "0.5", "less -- it's due a tails", "more -- hot streak",
                "The coin does not remember. The coin does not care."),
            new Question("A fraud model that always predicts 'no fraud' is 99% accurate. Why is that useless?",
                "class imbalance -- accuracy hides it", "it isn't, 99% is great", "needs more layers",
                "Accuracy is a vibe. Recall is a fact."),
        }));
    }
    static void course(Course c) { COURSES.put(c.key, c); }

    /** Core three needed to graduate. */
    static final String[] CORE_COURSES = { "cs101", "datastructures", "algorithms" };

    // ---------------------------------------------------------------- quests

    static class Quest {
        final String id, name, action; // action: work/study/project/team/learn/portfolio
        final int target, minLevel;
        int progress = 0;
        boolean done = false;
        Quest(String id, String name, String action, int target, int minLevel) {
            this.id = id; this.name = name; this.action = action;
            this.target = target; this.minLevel = minLevel;
        }
        int rewardMoney() { return 50 + target * 10; }
    }

    static List<Quest> defaultQuests() {
        List<Quest> q = new ArrayList<>();
        q.add(new Quest("cs101_project",  "Finish your CS101 project",            "project",   3,  1));
        q.add(new Quest("algo_exam",      "Cram for the algorithms exam",         "study",     4,  1));
        q.add(new Quest("first_site",     "Build your first website",             "project",   5,  1));
        q.add(new Quest("first_sprint",   "Finish your first sprint",             "work",      5, 17));
        q.add(new Quest("prod_bugs",      "Fix 5 production bugs",                "work",      5, 17));
        q.add(new Quest("demo_day",       "Prepare for demo day",                 "learn",     3, 17));
        q.add(new Quest("launch_app",     "Launch your first app",                "project",   8, 25));
        q.add(new Quest("code_review",    "Survive your first code review",       "team",      2, 25));
        q.add(new Quest("issue_2am",      "Fix a major production issue",         "work",      6, 25));
        q.add(new Quest("ship_ten",       "Ship 10 features",                     "work",     10, 25));
        q.add(new Quest("all_nighters",   "Pull 3 all-nighters",                  "project",   3, 25));
        q.add(new Quest("mentor_junior",  "Mentor the new junior",                "team",      2, 25));
        q.add(new Quest("master_craft",   "Master your craft",                    "learn",     3, 25));
        q.add(new Quest("standups",       "Survive 5 standups",                   "team",      5, 25));
        q.add(new Quest("learn_seniors",  "Learn from the seniors",               "learn",     4, 25));
        q.add(new Quest("portfolio_50",   "Build a portfolio worth talking about","portfolio",50, 25));
        return q;
    }

    static List<Quest> pathQuests(String path) {
        List<Quest> q = new ArrayList<>();
        switch (path) {
            case "backend":
                q.add(new Quest("pq_backend_1",  "Design the payments API",      "work",    6, 41));
                q.add(new Quest("pq_backend_2",  "Kill the N+1 query",           "project", 3, 41));
                break;
            case "frontend":
                q.add(new Quest("pq_frontend_1", "Ship the design system",       "project", 5, 41));
                q.add(new Quest("pq_frontend_2", "Make the bundle smaller",      "work",    5, 41));
                break;
            case "ai":
                q.add(new Quest("pq_ai_1",       "Ship a model to production",   "project", 6, 41));
                q.add(new Quest("pq_ai_2",       "Fix the data pipeline",        "work",    5, 41));
                break;
            case "devops":
                q.add(new Quest("pq_devops_1",   "Automate the deploy",          "work",    5, 41));
                q.add(new Quest("pq_devops_2",   "Survive the migration",        "project", 4, 41));
                break;
        }
        return q;
    }

    // ---------------------------------------------------------------- graduation paths (§8, 1.7)

    static final String[] GRAD_PATHS  = { "developer", "security", "qa", "data", "devops", "ai" };
    static final String[] GRAD_LABELS = {
        "Developer        -- You want to build things",
        "Security Analyst -- You're paranoid in the best way",
        "QA Engineer      -- You break things for a living",
        "Data Scientist   -- You see patterns in chaos",
        "DevOps           -- You care how it runs",
        "AI Engineer      -- You're chasing the frontier",
    };

    static String talkTitle(String gradPath) {
        switch (gradPath) {
            case "security": return "Things I Broke So You Don't Have To";
            case "qa":       return "It Worked On My Machine: A Post-Mortem";
            case "data":     return "Lies, Damned Lies, and Dashboards";
            case "devops":   return "The Server Is On Fire and That's Okay";
            case "ai":       return "The Model Is Confidently Wrong and So Am I";
            default:         return "Boring Code Ships: A Love Letter to the Obvious Solution";
        }
    }
}
