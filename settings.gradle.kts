rootProject.name = "sk-repo"

include(
    // zajednički podaci (formati config.json i metadata.json)
    "model",

    // biblioteka 1: kreiranje repozitorijuma i postavljanje komponenti
    "publisher-api",
    "local-publisher",
    "gdrive-publisher",
    "cli-publisher",

    // biblioteka 2: preuzimanje komponenti i zavisnosti
    "resolver-api",
    "local-resolver",
    "gdrive-resolver",
    "cli-resolver",
)
