# Agent Guidelines: Codebase Graph & Analysis

To maintain architectural clarity and understand dependencies, this project utilizes **Graphify**. All generated graph outputs, summaries, and structural maps are stored in the `graphify-out/` folder.

---

## 1. Reading Architectural Data

- **Source of Truth:** Always consult the files inside the `graphify-out/` folder when exploring module dependencies, cross-cutting components, or unfamiliar parts of the codebase.
- **Scope:** Rely on these pre-analyzed maps to trace references before making multi-file modifications or refactoring core layers.

---

## 2. Updating the Graph After Changes

- **Trigger condition:** Whenever you make structural changes (such as adding/deleting files, moving classes, or changing significant module imports/dependencies), you must update the graph.
- **Execution:** Run the graphify command to regenerate the contents of the `graphify-out/` folder:

```bash
graphify
```
