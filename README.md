# IFogSim Energy Labeling

Energy-labeling work on top of [IFogSim](https://github.com/saeedehbaneshi/IFogSim) (a CloudSim-based fog/edge computing simulator): running DCNS and VR-game topologies, extracting per-app/per-VM energy results, and producing labeled datasets (equal-width / percentile binning) for downstream analysis.

This folder is the canonical, up-to-date copy of this work — it matches `origin/main` on GitHub exactly (commit `26d04e4`, tag `EnergyLabeling-v2`) and has a clean working tree.

## History note

This project previously existed as **6 separate local clones** (`Simulators/iFog_Simulator/{IFogSim,IFogSimBackup,IFogSim_EnergyLabelingBackup,previous/iFogSim}`, `~/test`, `~/Downloads/test`) that had drifted apart over time — a symptom of not being tracked cleanly in git. Consolidated here on 2026-09-22:

- `legacy_jar_packaging_notes/` — a few small helper files (jar manifest, build readme, run script) found uncommitted in one of the old clones; kept for reference, never made it into a commit.
- `Report_Saeedeh/.../Final/alternate_variants/` — an alternate version of one results spreadsheet found in an old clone, differs from the committed version; kept in case it matters, not verified which is "correct."

**Not yet merged in:** `Simulators/iFog_Simulator/IFogSimBackup`, `previous/iFogSim`, and `IFogSim_EnergyLabelingBackup` still sit in their old location. They contain git history that was **never pushed to GitHub** (a separate local-only line of commits, tip `17352b1`, "Updated Energy Labeling with added JAR files" — pom.xml/Zenodo-packaging flavored work). Once GitHub auth is set up, that history should be pushed as an archive branch before those folders are cleaned up, so it isn't lost. `IFogSimBackup` also has some uncommitted analysis output (`LabellingResultParser.ipynb`, `Total_Labelling.csv`, a couple of results folders) worth reviewing before removal.
