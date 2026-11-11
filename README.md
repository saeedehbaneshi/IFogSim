# iFogSim with application-level and communication energy accounting

This repository contains the version of iFogSim that I extended and used in my PhD research
at the University of Amsterdam (Parallel Computing Systems group). It is based on
[iFogSim2](https://github.com/Cloudslab/iFogSim) from the CLOUDS Laboratory, University of
Melbourne, which is built on [CloudSim](https://github.com/Cloudslab/cloudsim). The commit
history before May 2023 comes from iFogSim2.

## What this version adds

The extensions are described in the papers listed under [Publications](#publications).

- **Application-level energy reporting.** Besides the total energy of each device, the
  simulator reports the energy a device spends while it is actively serving an application
  (WOCC'23).
- **Communication energy.** Energy models for network interface cards: a flow-based model
  for shared devices (fog nodes, cloud) and a time-based model for end-user devices, adapted
  to the space-shared links of the simulator. Networking energy is reported per tuple
  (FMEC'24).
- **Per-VM computation energy.** Energy is attributed to the VMs that host application
  modules, based on each VM's share of the device's MIPS, so that energy can be attributed to
  individual applications when several applications share the same devices (FMEC'24).
- **A fix in capacity allocation.** MIPS is also allocated to the VM that receives an arriving
  tuple. Without this, finish-time estimates could become infinite and some VMs reported too
  much energy (FMEC'24).
- **Energy-labelling scenarios** for the video surveillance (DCNS) application, used in
  CCGrid'25.

## Main files

| Path | Content |
|---|---|
| `src/org/fog/test/perfeval/LabellingDCNS.java` | Surveillance application and the 19 deployment scenarios used for energy labelling (CCGrid'25) |
| `src/org/fog/test/perfeval/TwoDCNSApps.java`, `HighResTwoDCNSApp.java`, `LowResTwoDCNSApp.java` | Two-application surveillance scenarios |
| `src/org/fog/entities/FogDevice.java`, `src/org/fog/placement/Controller.java`, `src/org/cloudbus/cloudsim/power/PowerDatacenter.java` | Most of the energy-accounting changes |
| `LabellingDCNS.jar` | Pre-built jar of `LabellingDCNS` |
| `jars/` | Java dependencies (CloudSim 3.0.3, Commons Math, Guava, json-simple) |
| `LabellingResultParser.ipynb`, `Merging_different_rate_results_labelling.ipynb` | Running all scenarios, parsing the output, and energy labelling |
| `Report_Saeedeh/Definig_labels_data_after_fixing_simulator_CCGRID/Final/` | Data behind the CCGrid'25 figures |

## Requirements

- Java 17 (OpenJDK 17 was used for the checks below)
- For the notebooks: Python 3 with `pandas`, `numpy`, `matplotlib` and Jupyter

## Running one scenario

With the pre-built jar, from the repository root:

```bash
java -jar LabellingDCNS.jar Cloud_Only
```

The results (energy per device and per application, networking energy, delays) are printed
to standard output. On the laptop where this was checked, `Cloud_Only` took about a second,
`Edge_Edge_Proxy` about 15 seconds and `Router_Based` about a minute.

Accepted scenario names:

```
Router_Only  Router_Proxy  Router_Cloud  Proxy_Only  Proxy_Cloud  Cloud_Only
Edge_Based  Cloud_Based  Router_Based  Proxy_Based
Router_Router_Proxy  Router_Router_Cloud  Proxy_Proxy_Cloud  Router_Proxy_Proxy
Router_Cloud_Cloud  Proxy_Cloud_Cloud  Edge_Edge_Router  Edge_Edge_Proxy  Edge_Edge_Cloud
```

## Building the labelling simulation from source

The labelling simulation can be compiled with `javac` and the jars in `jars/`:

```bash
CP=$(find jars -name '*.jar' | tr '\n' ':')
javac -encoding ISO-8859-1 -cp "$CP" -sourcepath src -d /tmp/ifogsim-classes \
      src/org/fog/test/perfeval/LabellingDCNS.java
java -cp "/tmp/ifogsim-classes:$CP" org.fog.test.perfeval.LabellingDCNS Cloud_Only
```

When this was checked (November 2026) for `Cloud_Only`, `Edge_Edge_Proxy` and
`Router_Based`, the output was the same as that of the pre-built jar, apart from the reported
execution time.

This command compiles only the classes that the labelling simulation needs. Compiling the
whole `src/` tree in one go currently fails, because some files in it are not compatible with
the modified classes.

The camera emission interval (20 ms by default) is set on line 530 of `LabellingDCNS.java`.
CCGrid'25 uses 15, 20 and 25 ms; after changing it, rebuild as above.

## Running all scenarios

`LabellingResultParser.ipynb` runs all scenarios and parses their output. Its first cell
defines `run_command` with absolute paths from the machine where it was written; change the
Java path and the class path to your own (for example, the class directory and jars from the
build above). `Merging_different_rate_results_labelling.ipynb` combines the results of the
three emission intervals.

## Archived versions

The tagged versions of this repository are archived on Zenodo
([all versions: doi:10.5281/zenodo.14944696](https://doi.org/10.5281/zenodo.14944696)).
The version used for CCGrid'25 is `EnergyLabeling-v2` (commit `26d04e4`):
[doi:10.5281/zenodo.14969629](https://doi.org/10.5281/zenodo.14969629).

The archives are fixed snapshots, and the `main` branch can contain later changes. When this
was checked (November 2026), these were documentation and a few added notes and data files;
the source code, the pre-built jar and the notebooks were the same as in `EnergyLabeling-v2`.

## Publications

- S. Baneshi, A. Pathania, B. Akesson, A. Pimentel, A.-L. Varbanescu. Empowering
  Sustainability: Energy Labeling of Digital Services Using Simulation. IEEE/ACM CCGrid 2025,
  pp. 580–585. [doi:10.1109/CCGRID64434.2025.00047](https://doi.org/10.1109/CCGRID64434.2025.00047)
- S. Baneshi, A. Pathania, B. Akesson, A. Pimentel, A.-L. Varbanescu. Analyzing
  Per-Application Energy Consumption in a Multi-Application Computing Continuum. FMEC 2024,
  pp. 30–37. [doi:10.1109/FMEC62297.2024.10710253](https://doi.org/10.1109/FMEC62297.2024.10710253)
- S. Baneshi, A.-L. Varbanescu, A. Pathania, B. Akesson, A. Pimentel. Estimating the Energy
  Consumption of Applications in the Computing Continuum with iFogSim. WOCC'23, ISC High
  Performance 2023 Workshops, LNCS, pp. 234–249.
  [doi:10.1007/978-3-031-40843-4_18](https://doi.org/10.1007/978-3-031-40843-4_18)

## Citation and credit

If you use this code, please cite the FMEC 2024 paper (see `CITATION.cff`), and also the
simulators it is based on:

- R. Mahmud, S. Pallewatta, M. Goudarzi, R. Buyya. iFogSim2: An extended iFogSim simulator for
  mobility, clustering, and microservice management in edge and fog computing environments.
  Journal of Systems and Software 190, 111351, 2022.
  [doi:10.1016/j.jss.2022.111351](https://doi.org/10.1016/j.jss.2022.111351)
- H. Gupta, A. V. Dastjerdi, S. K. Ghosh, R. Buyya. iFogSim: A toolkit for modeling and
  simulation of resource management techniques in the Internet of Things, Edge and Fog
  computing environments. Software: Practice and Experience 47(9), 1275–1296, 2017.
  [doi:10.1002/spe.2509](https://doi.org/10.1002/spe.2509)

## Licensing status

The repository-wide licensing of the changes made in this repository is under review, and no
license file has been added yet. Files that come from CloudSim and iFogSim keep their existing
license notices. Before redistributing the code, please check those notices and the licenses
of the upstream projects.

## Contact

Saeedeh Baneshi, baneshisaeedeh@gmail.com
