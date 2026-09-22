# SkyblockPlugin

Minecraft (Paper 26.2) plugin — SkyBlock s jednim (klasičnim) tipom otoka i podrškom za 2 igrača po otoku.

## Značajke
- **Jedan tip otoka — klasični**
  - Glavni travnati dio (dirt/grass, drvo, jezerce, lava, sanduk sa starter itemima)
  - Nekoliko manjih pješčanih otočića okolo (sand/sandstone, kaktusi, dead bush) — pustinjski detalj kao dodatak glavnom otoku, ne poseban tip
  - **Dvije krave** na glavnoj platformi, spremne za razmnožavanje (hrani ih pšenicom da dobiješ tele)
- **Otok za do 2 osobe** — vlasnik + 1 pozvani član (podesivo u `config.yml` preko `max-members`)
- Sustav pozivnica: `invite` / `accept` / `deny` / `kick` / `leave`
- Automatsko raspoređivanje otoka po spiralnoj mreži, bez preklapanja (`island-spacing` u configu)
- Zaštita otoka — samo vlasnik i član mogu graditi/rušiti unutar radijusa svog otoka (`protection-radius`)
- Spremanje podataka u `plugins/SkyblockPlugin/islands.yml` (preživljava restart servera)
- Odvojeni svijet (`skyblock_world`, flat generator) koji se kreira automatski

## Komande
| Komanda | Opis |
|---|---|
| `/island create` | Kreira novi otok |
| `/island home` | Teleport na tvoj otok |
| `/island invite <igrač>` | Pozovi drugog igrača (max 2 po otoku) |
| `/island accept` / `deny` | Prihvati / odbij pozivnicu |
| `/island kick <igrač>` | Izbaci člana (samo vlasnik) |
| `/island leave` | Napusti otok kao član |
| `/island delete` | Obriši svoj otok (samo vlasnik) |
| `/island info` | Info o tvom otoku |
| `/island help` | Pomoć |

Alias: `/is`, `/otok`

## Build (na tvom računalu, treba internet pristup)
Potreban je JDK 21+ i Maven (Minecraft 26.x zahtijeva Java 21).

```bash
mvn clean package
```

Nakon builda, `.jar` fajl će biti u `target/SkyblockPlugin.jar`.
Kopiraj ga u `plugins/` folder na Paper 26.2 serveru i restartaj server.

> Napomena: build treba internet pristup repozitoriju `repo.papermc.io` (za Paper API)
> i Maven Central, jer se ovaj sandbox okoliš ne može spojiti na te repozitorije.
> Zato je build napravljen kao gotov izvorni Maven projekt — pokreni `mvn package` lokalno.

## Konfiguracija (`config.yml`)
```yaml
world-name: "skyblock_world"
island-spacing: 250
island-y: 100
max-members: 2
protection-radius: 60
spawn-location: "world;0;100;0"
```

## Struktura projekta
```
skyblock-plugin/
  pom.xml
  src/main/resources/plugin.yml
  src/main/resources/config.yml
  src/main/java/com/example/skyblock/
    SkyblockPlugin.java
    island/IslandType.java
    island/Island.java
    island/IslandGenerator.java
    island/IslandManager.java
    commands/IslandCommand.java
    listeners/ProtectionListener.java
```
