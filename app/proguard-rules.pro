# App-specific R8 rules. :core ships its own consumer-rules.pro, and Hilt, Retrofit, OkHttp,
# kotlinx.serialization and Compose all ship consumer rules inside their artifacts, so this file
# only needs whatever :app itself puts beyond reflection's reach.
