# 0003. Typography

Date: 2026-09-19
Status: accepted
Step: 1

## Decision

Three families, all SIL OFL 1.1, all variable fonts shipped as one file each:

| Role | Family | Used for |
| --- | --- | --- |
| Display | Bodoni Moda | The four large words, screen titles |
| Label | Jost | Capitals with wide tracking: dates, status, control text |
| Body | Literata | Reading and writing |

## One weight per family

The app loads the default instance of each variable font and never asks for a
second weight.

Reasons:

1. E-ink rule: a thin hairline breaks up on the panel, and a heavy cut fills in
   and ghosts. A Bodoni at Black or at Thin is a bad idea on 16 grey levels.
2. Compose can set variable font axes, but a wrong axis value gives a silent
   fallback that is hard to see without the device. One instance removes a
   whole class of problem we cannot test here.

Emphasis comes from size, space and small capitals instead of weight.

Revisit this in Step 10 with the tablet in hand. If a heavier title is wanted,
add `FontVariation.Settings` and check it on the panel first, not in an
emulator.

## Sizes

See `design/EinkType.kt`. The scale is short on purpose: word 52, title 34,
row title 20, body 17, reading 19, caps 13, caps small 11.
