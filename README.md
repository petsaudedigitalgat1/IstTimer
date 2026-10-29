# IST Timer

Aplicativo Android de temporizadores, desenvolvido pelo **PET Saúde Digital** (GAT 01).

- **Application ID:** `io.github.petsaudedigitalgat1.isttimer`
- **Android mínimo:** 5.0 (API 21) · **Target:** API 34
- **Idiomas:** português (padrão), inglês e espanhol

## Requisitos

- **JDK 17 ou superior** (o JDK que vem com o Android Studio já serve)
- **Android SDK** com a plataforma 34 instalada
- Android Studio (opcional, mas recomendado)

Se for compilar pela linha de comando sem o Android Studio, crie o arquivo `local.properties` na raiz do projeto apontando para o SDK:

```properties
sdk.dir=/caminho/para/Android/Sdk
```

> Nos comandos abaixo, use `./gradlew` no Linux/macOS e `gradlew.bat` no Windows.

## APK de teste (debug)

Use para testar no celular ou no emulador durante o desenvolvimento.

```bash
./gradlew assembleDebug
```

O APK é gerado em:

```
app/build/outputs/apk/debug/app-debug.apk
```

Para instalar direto em um aparelho conectado por USB (com a depuração USB ativada):

```bash
./gradlew installDebug
# ou
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Também é possível copiar o `.apk` para o celular e abrir pelo gerenciador de arquivos. Nesse caso o Android pede permissão para instalar apps de fontes desconhecidas.

No Android Studio: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.

## APK de release

A versão de release é otimizada para distribuição e não é depurável.

```bash
./gradlew assembleRelease
```

O APK é gerado em:

```
app/build/outputs/apk/release/app-release.apk
```

### Assinatura

Todo APK precisa ser assinado. A configuração está em `app/build.gradle`, no bloco `signingConfigs`:

| Build   | Keystore                | Alias             |
|---------|-------------------------|-------------------|
| debug   | `app/debug.keystore`    | `androiddebugkey` |
| release | `app/release.keystore`  | `release`         |

A keystore de debug está no repositório. A de release **não está** (ela fica no `.gitignore`): peça-a ao responsável pelo projeto e coloque-a em `app/release.keystore` antes de gerar o APK de release. Sem ela, o `assembleRelease` falha.

**O Android só instala uma atualização se ela tiver sido assinada com a mesma chave da versão instalada.** Se a keystore de release for perdida, não é mais possível atualizar o app para quem já o instalou. Guarde-a em um lugar seguro, fora do repositório.

Para criar uma keystore nova:

```bash
keytool -genkeypair -v -keystore app/release.keystore -alias release \
  -keyalg RSA -keysize 2048 -validity 10000
```

Depois atualize `storePassword` e `keyPassword` em `app/build.gradle`.

## Publicação na Play Store

A Play Store exige o formato **AAB** (Android App Bundle) em vez de APK:

```bash
./gradlew bundleRelease
```

O arquivo é gerado em `app/build/outputs/bundle/release/app-release.aab`.

Antes de cada envio:

1. Aumente o `versionCode` em `app/build.gradle`. A Play Store recusa um envio com um `versionCode` que já foi usado. Atualize também o `versionName`, que é a versão mostrada ao usuário.
2. Ative o **Play App Signing** no Play Console. Assim o Google guarda a chave final do app, e a keystore do projeto passa a ser só uma chave de upload, que pode ser trocada se for perdida.

O `applicationId` **não pode ser alterado** depois da primeira publicação.

## Testes

```bash
./gradlew testDebugUnitTest
```

O relatório fica em `app/build/reports/tests/testDebugUnitTest/index.html`.

## Estrutura

```
app/src/main/java/io/github/petsaudedigitalgat1/isttimer/
├── adapter/    # adapters do RecyclerView
├── model/      # modelos de dados
├── receiver/   # BroadcastReceivers dos alarmes
├── ui/         # Activities
├── util/       # agendamento, persistência e reprodução de alarmes
└── viewmodel/  # ViewModels
```
