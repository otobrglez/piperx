# piperx

This is Scala 3 experiment with Cats Effect and FS2.

Trying to do fast and efficient CSV upload, nothing more.

## Experiments

- Fake CSV data can be generated
  with [GenerateCSVsApp.scala](src/main/scala/com/github/otobrglez/piperx/GenerateCSVsApp.scala)
- Upload is demonstrated with [UploadApp.scala](src/main/scala/com/github/otobrglez/piperx/UploadApp.scala)
- Transformation with streaming download and upload is in [TransformApp.scala](src/main/scala/com/github/otobrglez/piperx/TransformApp.scala)

## Notes

- Make sure you boot-up Docker Compose before run: `docker compose -f docker-compose.yml up`
- Use `sbt` for running and experimenting with this thing.
- This project does NOT use real S3 but rather [localstack](https://github.com/localstack/localstack)

\- Oto Brglez