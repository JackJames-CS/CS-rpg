# CS Career RPG

A text-based RPG played entirely in the terminal. You're a broke CS student in
Dublin with a laptop that sounds like a hairdryer, and ten years to tell the
long version of how you got into tech.

## Run

Requires a JDK (Java 17+).

```
run.bat
```

Or manually:

```
javac -encoding UTF-8 *.java
java RPG
```

## The shape of it

| Phase | Levels | Story |
|---|---|---|
| Student | 1–16 | Four college years. Courses, exams, a hackathon, graduation. |
| Intern | 17–24 | GigaCorp, StartupX, or PublicSector. Ends with The Review. |
| Junior | 25–40 | Imposter syndrome, a terrible Tuesday, pints with Ciarán. |
| Senior | 41–50 | Staff or Lead. Ends with Three Doors. |

Type `help` in game. Exams test actual CS — the game assumes you're a CS
student and is quietly revising you.

## Design docs

The story bible and design docs live in the vault (not this repo). The story
is a linear spine with persistent echoes: choices set flags, flags open and
close doors, and every choice pays off at least once before the end.
