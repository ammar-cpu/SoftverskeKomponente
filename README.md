# Repozitorijum softverskih komponenti

Projekat iz Softverskih komponenti, RAF 2026/27.

Repozitorijum komponenti po uzoru na Maven i dve biblioteke za rad sa njim: jedna pravi
repozitorijum i postavlja komponente, druga ih preuzima zajedno sa tranzitivnim zavisnostima.
Repozitorijum može biti na lokalnom disku, na drugom računaru u mreži ili na Google Drive-u.
Konkretna implementacija se učitava preko SPI mehanizma, kao JDBC drajver.

Formati fajlova i ostali dogovori su u [DESIGN.md](DESIGN.md).

## Moduli

| modul | šta je |
|---|---|
| `model` | zajednički podaci: oznaka komponente, config.json, metadata.json, raspored foldera |
| `publisher-api` | biblioteka 1: kreiranje repozitorijuma, postavljanje, pretraga |
| `local-publisher`, `network-publisher`, `gdrive-publisher` | implementacije biblioteke 1 |
| `cli-publisher` | program za komandnu liniju za biblioteku 1 |
| `resolver-api` | biblioteka 2: preuzimanje, tranzitivne zavisnosti, konflikti verzija |
| `local-resolver`, `network-resolver`, `gdrive-resolver` | implementacije biblioteke 2 |
| `cli-resolver` | program za komandnu liniju za biblioteku 2 |
| `network-server` | server koji se pokreće na drugom računaru |

CLI programi se kompajliraju samo protiv API-ja. Implementacije su `runtimeOnly` zavisnosti
i nalaze se preko `ServiceLoader`-a.

## Build

Projekat je u Kotlinu. Potreban je JDK 17 ili noviji. Gradle ne mora biti instaliran, koristi se wrapper.

```
./gradlew build                 # kompajliranje i testovi
./gradlew dokkaGenerate         # dokumentacija (Dokka), u <modul>/build/dokka/html
./gradlew installDist           # programi spremni za pokretanje
```

Na Windows-u umesto `./gradlew` ide `gradlew.bat`.

## Pokretanje

Posle `installDist` programi su u `cli-publisher/build/install/cli-publisher/bin/`
i `cli-resolver/build/install/cli-resolver/bin/`.

Planirane komande:

```
cli-publisher local ./moj-repo init jar,zip
cli-publisher local ./moj-repo deploy metadata.json app-1.0.0.jar

cli-resolver local ./moj-repo spisak.txt ./preuzeto --strategy=nearest
```

## Google Drive

`credentials.json` i sačuvani tokeni se nikad ne commit-uju (već su u `.gitignore`).
