<h1 align="center">Ubahkan</h1>

<p align="center"><strong>Konversi file, tanpa internet.</strong></p>

<p align="center">
  Video, audio, gambar, dan dokumen diubah langsung di HP kamu.<br>
  Tanpa unggah. Tanpa akun. Tanpa izin internet sama sekali.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/100%25-offline-1565C0" alt="100% offline">
  <img src="https://img.shields.io/badge/Android-8.0%2B-141414" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/lisensi-GPL--3.0-83837B" alt="GPL-3.0">
</p>

---

## Kenapa Ubahkan?

Aplikasi konverter umumnya bekerja dengan mengunggah berkasmu ke server orang lain. Ubahkan tidak begitu. Seluruh proses berjalan di dalam perangkatmu memakai mesin FFmpeg, jadi:

- Berkas pribadi tidak pernah meninggalkan HP kamu
- Tetap jalan di mode pesawat, di kereta, di tempat tanpa sinyal
- Tidak ada antre server, tidak ada batas ukuran dari pihak ketiga
- Tidak perlu daftar, tidak ada iklan pelacak

## Yang bisa dilakukan sekarang

**Video**  -  ubah format (MP4, MKV, MOV, WEBM, AVI, GIF), ambil audionya saja, mode Instan untuk pindah wadah tanpa mengubah kualitas.

**Audio**  -  ubah format (MP3, WAV, FLAC, AAC, M4A, OGG, OPUS) dengan pilihan kualitas.

**Gambar**  -  ubah format gambar, jadikan PDF, dan sebaliknya: halaman PDF jadi gambar, perkecil ukuran PDF, SVG jadi PNG/JPG/WEBP.

**Dokumen**  -  EPUB jadi TXT, gambar dan PDF saling tukar.

**Cara kerja sehari-hari**  -  banyak berkas diproses sekaligus (dikelompokkan otomatis per format asal), berkas bisa dikirim langsung dari aplikasi lain lewat menu Bagikan, proses jalan di latar belakang dengan notifikasi progres, dan semua hasil rapi di satu folder: `Downloads/Ubahkan`.

## Sedang dibangun

Peta jalan rilis berikutnya, dikerjakan terbuka di repo ini:

- Kompres ke target ukuran (mis. video di bawah 10 MB, foto di bawah 200 KB)
- Potong otomatis: video panjang dibagi per 30 detik / 1 menit / 1,5 menit, siap bagikan ke Status WhatsApp
- Potong (trim), gabung video, gabung dan pisah PDF, bakar subtitle ke video
- Crop rasio 9:16 / 1:1, ubah resolusi, bisukan atau ganti audio video
- Modul Pindai Dokumen ala pemindai saku: deteksi tepi otomatis, luruskan perspektif, banyak halaman, filter dokumen, OCR
- Tampilan informatif: info berkas lengkap sebelum konversi, penjelasan tiap format, estimasi hasil, dan laporan sebelum vs sesudah

## Cara pakai

1. Buka Ubahkan, ketuk **Pilih File**
2. Pilih format tujuan dan kualitas
3. Ketuk **Konversi Sekarang**  -  hasil ada di `Downloads/Ubahkan`

Panduan lengkap per fitur tersedia di dalam aplikasi, menu **Panduan**.

## Unduh

APK tersedia di halaman [Actions](https://github.com/dandienter/ubahkan/actions) (artefak build terbaru) selama rilis resmi disiapkan.

## Teknologi

Kotlin · Jetpack Compose · FFmpegKit Full · Android PdfRenderer  -  semua pemrosesan lokal di perangkat.

## Lisensi dan asal-usul

Ubahkan adalah perangkat lunak bebas berlisensi **GPL-3.0** (lihat `LICENSE`).

Ubahkan merupakan fork dari proyek [Morph](https://github.com/RizkyFauzi06/Morph) karya Rizki Fauzi, yang dirombak total dari segi nama, tampilan, dan arah fitur. Sesuai ketentuan GPL-3.0, pemberitahuan hak cipta asli dipertahankan di dalam kode sumber dan berkas `NOTICE.md`; kode sumber Ubahkan tersedia terbuka di repo ini dengan lisensi yang sama.

## Kredit

Ubahkan oleh [@dandisubhani_](https://www.instagram.com/dandisubhani_)
