# Dogovor oko formata

Ovo je zajednički izvor istine za obe biblioteke. Ako nešto ovde menjate, prvo se dogovorite,
jer biblioteka za preuzimanje čita tačno ono što biblioteka za postavljanje piše.

Stavke označene sa **(predlog)** još nisu potvrđene.

## Struktura repozitorijuma

```
koren/
├── config.json
└── rs.raf/                     organizacija je jedan folder, ne rs/raf/
    └── utils/                  projekat
        └── 2.1.0/              verzija
            ├── metadata.json
            └── utils-2.1.0.jar
```

- Organizacija je jedan folder. Na Google Drive-u je svaki nivo foldera dodatno traženje po imenu,
  pa je manje nivoa bolje.
- Ime fajla komponente je `<projekat>-<verzija>.<ekstenzija>`.
- Putanje se sastavljaju na jednom mestu u modulu `model`, a ne ručno po kodu.

## config.json

```json
{
  "allowedExtensions": ["jar", "zip"],
  "scopes": {
    "compile": true,
    "runtime": true,
    "test": false,
    "provided": false
  }
}
```

- `true` znači da se zavisnosti tog opsega povlače tranzitivno.
- Ekstenzije se pišu bez tačke. Poređenje ne razlikuje velika i mala slova.
- Konfiguracija može da se menja posle inicijalizacije, ali nova pravila važe samo za
  komponente postavljene posle izmene. **(predlog)**
- Čita ga i biblioteka za preuzimanje, da bi znala koji opsezi su tranzitivni.

## metadata.json

```json
{
  "organization": "rs.raf",
  "project": "app",
  "version": "1.0.0",
  "file": "app-1.0.0.jar",
  "dependencies": [
    { "organization": "rs.raf", "project": "utils", "version": "2.1.0", "scope": "compile" },
    { "organization": "org.junit", "project": "junit", "version": "5.10.0", "scope": "test" },
    { "organization": "rs.raf", "project": "log", "version": "1.0.0", "scope": "compile", "optional": true }
  ]
}
```

- Polja zavisnosti su odvojena, a ne jedan string, da bi provera "navedeni su svi podaci" bila jednostavna.
- Ako `optional` nije naveden, vrednost je `false`.

## Oznaka komponente

- Tekstualni oblik: `organizacija:projekat:verzija`, npr. `rs.raf:utils:2.1.0`.
- Nijedan deo ne sme biti prazan niti sadržati `:` ili `/`.

## Verzije **(predlog)**

- Samo brojevi razdvojeni tačkama: `1`, `1.2`, `1.2.0`. Ostalo se odbija pri postavljanju.
- Porede se deo po deo kao brojevi: `1.10.0` je novija od `1.9.0`.
- Deo koji fali se tretira kao 0: `1.2` i `1.2.0` su iste verzije.

## Spisak za preuzimanje

```
# komentar
rs.raf:app:1.0.0
rs.raf:parser:3.0.0
```

Jedna komponenta po liniji. Prazne linije i linije koje počinju sa `#` se preskaču.
Sve komponente sa spiska imaju dubinu 1 u stablu zavisnosti.

## Lokacija po tipu repozitorijuma

| tip       | lokacija                     | primer          |
|-----------|------------------------------|-----------------|
| `local`   | putanja do korenskog foldera | `./moj-repo`    |
| `network` | `host:port` servera          | `192.168.1.5:8080` |
| `gdrive`  | ime korenskog foldera na Drive-u | `sk-repo`   |

## Skladište

Svaka biblioteka ima svoj interfejs skladišta:

- `PublisherStorage` (publisher-api): `exists`, `read`, `write`, `list`, `createDir`
- `ResolverStorage` (resolver-api): `exists`, `read`, `list`. Bez upisa, jer preuzimanje nikad ne menja repozitorijum.

Pravila za oba:
- putanje su relativne u odnosu na koren, sa `/` kao separatorom; prazan string je koren
- `list` vraća samo imena, sortirana, i praznu listu ako folder ne postoji

## Mrežni server **(predlog)**

`network-server <port> <folder>` čuva repozitorijum u svom folderu i odgovara na HTTP zahteve:

| zahtev | odgovor |
|---|---|
| `GET /exists?path=...` | `true` ili `false` |
| `GET /read?path=...` | sadržaj fajla, 404 ako ne postoji |
| `GET /list?path=...` | imena, jedno po liniji |
| `POST /write?path=...` (telo je sadržaj) | 200 |
| `POST /mkdir?path=...` | 200 |

Server mora da odbije putanju sa `..`, da niko ne bi čitao fajlove van foldera repozitorijuma.

## Greške **(predlog)**

Sve greške su `RepositoryException` ili njene podklase iz modula `model`:
- `ValidationException`: komponenta ne prolazi provere pri postavljanju
- `ComponentNotFoundException`: tražena komponenta ne postoji

CLI ih hvata i ispisuje poruku.

## Granični slučajevi

- postavljanje verzije koja već postoji: greška, ništa se ne prepisuje
- zavisnost ne postoji u repozitorijumu: greška
- scope nije u config.json: greška
- komponenta sa spiska ne postoji pri preuzimanju: greška **(predlog)**
- ista komponenta na istoj dubini u dve verzije, strategija nearest: pobeđuje ona na koju se prvo naišlo

## Pitanja za asistenta

- Da li mrežni repozitorijum može da se demonstrira sa serverom na `localhost` ili mora na drugom računaru?
- Da li je u redu da je zajednički kod (formati fajlova, raspored foldera) u posebnom modulu `model` koji koriste obe biblioteke?
