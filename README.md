# MetaLab

MetaLab è una prima base Android nativa, ottimizzata per tablet, per un futuro laboratorio sperimentale audio e visivo. Il progetto usa Kotlin, Jetpack Compose e Material 3.

## Natura sperimentale

MetaLab non deve essere usata per affermare o dimostrare comunicazioni paranormali. Rumori, parole riconosciute, immagini e variazioni visive possono avere spiegazioni ordinarie.

> MetaLab è uno strumento sperimentale. I risultati non costituiscono prova di comunicazioni paranormali. Rumori, trascrizioni e variazioni visive possono dipendere da interferenze, riverbero, errori software, compressione, riflessi, polvere, insetti, cambi di luce o interpretazione soggettiva.

## Funzioni presenti nella fase 1

- interfaccia Android nativa scura e adattiva per tablet;
- layout a una o due colonne in base alla larghezza disponibile;
- campo multilinea per la domanda, limite di 500 caratteri e contatore;
- conservazione della domanda durante rotazione tramite `rememberSaveable`;
- cancellazione manuale della domanda;
- richiesta reale e separata dei permessi microfono e fotocamera;
- richiesta combinata dei due permessi;
- stati distinti: non richiesto, concesso, negato e negato definitivamente;
- messaggio di spiegazione dopo il rifiuto;
- apertura delle impostazioni Android dell'app quando un permesso è negato definitivamente;
- aggiornamento dello stato dei permessi al ritorno dalle impostazioni;
- pannelli informativi inattivi per Metafonia, Metavisione, Sessione combinata e Archivio;
- disclaimer sempre visibile;
- icona launcher, icona rotonda, icona adattiva e logo originali;
- workflow GitHub Actions per APK debug.

## Funzioni non implementate

In questa fase non sono presenti:

- registrazione audio;
- generazione di rumore o tono;
- riproduzione audio;
- trascrizione;
- attivazione o anteprima della fotocamera;
- analisi dei fotogrammi;
- rilevamento di movimento, forme o variazioni luminose;
- diario e salvataggio delle sessioni;
- esportazione di rapporti;
- risultati paranormali simulati.

I pannelli futuri mostrano soltanto lo stato reale delle funzioni e non contengono pulsanti finti.

## Permessi

Il Manifest dichiara esclusivamente:

- `android.permission.RECORD_AUDIO`;
- `android.permission.CAMERA`.

L'hardware fotografico è dichiarato come non obbligatorio. Non vengono richiesti permessi per Internet, archiviazione, posizione, notifiche, Bluetooth o identificatori pubblicitari.

La concessione dei permessi non avvia il microfono e non apre la fotocamera: prepara soltanto le fasi successive.

## Privacy

Nella fase 1:

- nessun audio viene registrato;
- nessuna immagine viene acquisita;
- la domanda non viene salvata in un database;
- nessun dato viene inviato online;
- nessun account è richiesto;
- non sono presenti pubblicità, telemetria o analytics.

## Base tecnica

- Kotlin 2.0.21
- Android Gradle Plugin 8.7.3
- Gradle 8.9 nel workflow GitHub Actions
- Java 17
- Jetpack Compose con BOM 2024.12.01
- Material 3
- compileSdk 35
- targetSdk 35
- minSdk 26

AGP 8.7 supporta API 35 e richiede Gradle 8.9 e JDK 17.

## Gradle Wrapper

Il progetto non include ancora il Gradle Wrapper completo (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar` e `gradle-wrapper.properties`). Non vengono creati wrapper incompleti o falsi.

GitHub Actions installa Gradle 8.9 tramite `gradle/actions/setup-gradle`.

## GitHub Actions e APK

Il workflow esegue:

```text
gradle projects --stacktrace
gradle assembleDebug --stacktrace
```

La build deve fallire se l'APK non esiste. Dopo una build verde, l'Artifact è:

```text
MetaLab-debug-<nome-ramo>
```

Percorso dell'APK nell'Artifact:

```text
app/build/outputs/apk/debug/app-debug.apk
```

La compilazione non va considerata riuscita finché GitHub Actions non mostra esito verde e l'APK non è realmente disponibile.

## Limiti reali

La distinzione fra rifiuto normale e rifiuto definitivo dipende dal comportamento del sistema Android e dal numero di richieste già effettuate. Alcuni produttori personalizzano le finestre dei permessi.

L'interfaccia è progettata per tablet e resta utilizzabile su schermi più stretti, ma deve essere verificata su dispositivi reali in verticale e orizzontale.

## Roadmap

### Fase 1

- base Android;
- grafica tablet;
- domanda scritta;
- permessi microfono e fotocamera;
- APK debug.

### Fase 2

- generazione reale di rumore bianco, rosa e marrone;
- tono regolabile;
- durata;
- volume;
- riproduzione.

### Fase 3

- registrazione microfono;
- salvataggio audio originale;
- Play, Pausa e Stop;
- gestione sessione.

### Fase 4

- trascrizione sperimentale;
- segmentazione;
- livello di incertezza;
- possibilità di “nessuna parola riconoscibile”;
- modalità cieca.

### Fase 5

- fotocamera;
- anteprima;
- movimento;
- variazioni luminose;
- confronto fotogrammi;
- cattura immagine;
- spiegazioni ordinarie.

### Fase 6

- sessione combinata;
- archivio;
- diario;
- esportazione rapporto.
