/*
 * Copyright (C) 2009,2010,2011,2012,2013 Samuel Audet
 *
 * This file is part of JavaCV.
 *
 * JavaCV is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * JavaCV is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaCV.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Based on the output-example.c file included in FFmpeg 0.6.5
 * as well as on the decoding_encoding.c file included in FFmpeg 0.11.1,
 * which are covered by the following copyright notice:
 *
 * Libavformat API example: Output a media file in any supported
 * libavformat format. The default codecs are used.
 *
 * Copyright (c) 2001,2003 Fabrice Bellard
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.qd.recorder;

import com.googlecode.javacpp.BytePointer;
import com.googlecode.javacpp.DoublePointer;
import com.googlecode.javacpp.FloatPointer;
import com.googlecode.javacpp.IntPointer;
import com.googlecode.javacpp.Loader;
import com.googlecode.javacpp.Pointer;
import com.googlecode.javacpp.PointerPointer;
import com.googlecode.javacpp.ShortPointer;
import com.googlecode.javacv.FrameRecorder;

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Map.Entry;

import static com.googlecode.javacv.cpp.avcodec.*;
import static com.googlecode.javacv.cpp.avformat.*;
import static com.googlecode.javacv.cpp.avutil.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.swresample.*;
import static com.googlecode.javacv.cpp.swscale.*;

/**
 *
 * @author Samuel Audet
 */
public class FFmpegFrameRecorder extends FrameRecorder {

    private String filename;
    private AVFrame picture;
    private AVFrame tmpPicture;
    private BytePointer pictureBuf;
    private BytePointer videoOutbuf;
    private int videoOutbufSize;
    private AVFrame frame;
    private Pointer[] samplesIn;
    private BytePointer[] samplesOut;
    private PointerPointer samplesInPtr;
    private PointerPointer samplesOutPtr;
    private BytePointer audioOutbuf;
    private int audioOutbufSize;
    private int audioInputFrameSize;
    private AVOutputFormat oformat;
    private AVFormatContext oc;
    private AVCodec video_codec;
    private AVCodec audio_codec;
    private AVCodecContext videoC;
    private AVCodecContext audioC;
    private AVStream videoSt;
    private AVStream audioSt;
    private SwsContext imgConvertCtx;
    private SwrContext samplesConvertCtx;
    private AVPacket videoPkt;
    private AVPacket audioPkt;
    private int[] gotVideoPacket;
    private int[] gotAudioPacket;
    private static Exception loadingException;

    public FFmpegFrameRecorder(File file, int audioChannels) {
        this(file, 0, 0, audioChannels);
    }

    public FFmpegFrameRecorder(String filename, int audioChannels) {
        this(filename, 0, 0, audioChannels);
    }

    public FFmpegFrameRecorder(File file, int imageWidth, int imageHeight) {
        this(file, imageWidth, imageHeight, 0);
    }

    public FFmpegFrameRecorder(String filename, int imageWidth, int imageHeight) {
        this(filename, imageWidth, imageHeight, 0);
    }

    public FFmpegFrameRecorder(File file, int imageWidth, int imageHeight, int audioChannels) {
        this(file.getAbsolutePath(), imageWidth, imageHeight);
    }

    public FFmpegFrameRecorder(String filename, int imageWidth, int imageHeight, int audioChannels) {
        this.filename      = filename;
        this.imageWidth    = imageWidth;
        this.imageHeight   = imageHeight;
        this.audioChannels = audioChannels;

        this.pixelFormat   = AV_PIX_FMT_NONE;
        this.videoCodec    = AV_CODEC_ID_NONE;
        this.videoBitrate  = 400000;
        this.frameRate     = 30;

        this.sampleFormat  = AV_SAMPLE_FMT_NONE;
        this.audioCodec    = AV_CODEC_ID_NONE;
        this.audioBitrate  = 64000;
        this.sampleRate    = 44100;

        this.interleaved = true;

        this.videoPkt = new AVPacket();
        this.audioPkt = new AVPacket();
    }

    public static FFmpegFrameRecorder createDefault(File f, int w, int h)   throws Exception { return new FFmpegFrameRecorder(f, w, h); }
    public static FFmpegFrameRecorder createDefault(String f, int w, int h) throws Exception { return new FFmpegFrameRecorder(f, w, h); }

    public static void tryLoad() throws Exception {
        if (loadingException != null) {
            throw loadingException;
        } else {
            try {
                Loader.load(com.googlecode.javacv.cpp.avutil.class);
                Loader.load(com.googlecode.javacv.cpp.avcodec.class);
                Loader.load(com.googlecode.javacv.cpp.avformat.class);
                Loader.load(com.googlecode.javacv.cpp.swscale.class);
            } catch (Throwable t) {
                if (t instanceof Exception) {
                    throw loadingException = (Exception)t;
                } else {
                    throw loadingException = new Exception("Failed to load " + FFmpegFrameRecorder.class, t);
                }
            }
        }
    }

    static {
        /* initialize libavcodec, and register all codecs and formats */
        av_register_all();
        avformat_network_init();
    }

    public void release() throws Exception {
        synchronized (com.googlecode.javacv.cpp.avcodec.class) {
            releaseUnsafe();
        }
    }
    public void releaseUnsafe() throws Exception {
        /* close each codec */
        if (videoC != null) {
            avcodec_close(videoC);
            videoC = null;
        }
        if (audioC != null) {
            avcodec_close(audioC);
            audioC = null;
        }
        if (pictureBuf != null) {
            av_free(pictureBuf);
            pictureBuf = null;
        }
        if (picture != null) {
            avcodec_free_frame(picture);
            picture = null;
        }
        if (tmpPicture != null) {
            avcodec_free_frame(tmpPicture);
            tmpPicture = null;
        }
        if (videoOutbuf != null) {
            av_free(videoOutbuf);
            videoOutbuf = null;
        }
        if (frame != null) {
            avcodec_free_frame(frame);
            frame = null;
        }
        if (samplesOut != null) {
            for (int i = 0; i < samplesOut.length; i++) {
                av_free(samplesOut[i].position(0));
            }
            samplesOut = null;
        }
        if (audioOutbuf != null) {
            av_free(audioOutbuf);
            audioOutbuf = null;
        }
        videoSt = null;
        audioSt = null;

        if (oc != null && !oc.isNull()) {
            if ((oformat.flags() & AVFMT_NOFILE) == 0) {
                /* close the output file */
                avio_close(oc.pb());
            }

            /* free the streams */
            int nbStreams = oc.nb_streams();
            for(int i = 0; i < nbStreams; i++) {
                av_free(oc.streams(i).codec());
                av_free(oc.streams(i));
            }

            /* free the stream */
            av_free(oc);
            oc = null;
        }

        if (imgConvertCtx != null) {
            sws_freeContext(imgConvertCtx);
            imgConvertCtx = null;
        }

        if (samplesConvertCtx != null) {
            swr_free(samplesConvertCtx);
            samplesConvertCtx = null;
        }
    }
    @Override protected void finalize() throws Throwable {
        super.finalize();
        release();
    }

    @Override public int getFrameNumber() {
        return picture == null ? super.getFrameNumber() : (int)picture.pts();
    }
    @Override public void setFrameNumber(int frameNumber) {
        if (picture == null) { super.setFrameNumber(frameNumber); } else { picture.pts(frameNumber); }
    }

    // best guess for timestamp in microseconds...
    @Override public long getTimestamp() {
        return Math.round(getFrameNumber() * 1000000L / getFrameRate());
    }
    @Override public void setTimestamp(long timestamp)  {
        setFrameNumber((int)Math.round(timestamp * getFrameRate() / 1000000L));
    }

    public void start() throws Exception {
        synchronized (com.googlecode.javacv.cpp.avcodec.class) {
            startUnsafe();
        }
    }
    public void startUnsafe() throws Exception {
        picture = null;
        tmpPicture = null;
        pictureBuf = null;
        frame = null;
        videoOutbuf = null;
        audioOutbuf = null;
        oc = null;
        videoC = null;
        audioC = null;
        videoSt = null;
        audioSt = null;
        gotVideoPacket = new int[1];
        gotAudioPacket = new int[1];

        /* auto detect the output format from the name. */
        String formatName = format == null || format.length() == 0 ? null : format;
        if ((oformat = av_guess_format(formatName, filename, null)) == null) {
            int proto = filename.indexOf("://");
            if (proto > 0) {
                formatName = filename.substring(0, proto);
            }
            if ((oformat = av_guess_format(formatName, filename, null)) == null) {
                throw new Exception("av_guess_format() error: Could not guess output format for \"" + filename + "\" and " + format + " format.");
            }
        }
        formatName = oformat.name().getString();

        /* allocate the output media context */
        if ((oc = avformat_alloc_context()) == null) {
            throw new Exception("avformat_alloc_context() error: Could not allocate format context");
        }

        oc.oformat(oformat);
        oc.filename().putString(filename);

        /* add the audio and video streams using the format codecs
           and initialize the codecs */

        if (imageWidth > 0 && imageHeight > 0) {
            if (videoCodec != AV_CODEC_ID_NONE) {
                oformat.video_codec(videoCodec);
            } else if ("flv".equals(formatName)) {
                oformat.video_codec(AV_CODEC_ID_FLV1);
            } else if ("mp4".equals(formatName)) {
                oformat.video_codec(AV_CODEC_ID_MPEG4);
            } else if ("3gp".equals(formatName)) {
                oformat.video_codec(AV_CODEC_ID_H263);
            } else if ("avi".equals(formatName)) {
                oformat.video_codec(AV_CODEC_ID_HUFFYUV);
            }

            /* find the video encoder */
            if ((video_codec = avcodec_find_encoder_by_name(videoCodecName)) == null &&
                (video_codec = avcodec_find_encoder(oformat.video_codec())) == null) {
                release();
                throw new Exception("avcodec_find_encoder() error: Video codec not found.");
            }

            AVRational frameRate = av_d2q(this.frameRate, 1001000);
            AVRational supportedFramerates = video_codec.supported_framerates();
            if (supportedFramerates != null) {
                int idx = av_find_nearest_q_idx(frameRate, supportedFramerates);
                frameRate = supportedFramerates.position(idx);
            }

            /* add a video output stream */
            if ((videoSt = avformat_new_stream(oc, video_codec)) == null) {
                release();
                throw new Exception("avformat_new_stream() error: Could not allocate video stream.");
            }
            videoC = videoSt.codec();
            videoC.codec_id(oformat.video_codec());
            videoC.codec_type(AVMEDIA_TYPE_VIDEO);

            /* put sample parameters */
            videoC.bit_rate(videoBitrate);
            /* resolution must be a multiple of two, but round up to 16 as often required */
            videoC.width((imageWidth + 15) / 16 * 16);
            videoC.height(imageHeight);
            /* time base: this is the fundamental unit of time (in seconds) in terms
               of which frame timestamps are represented. for fixed-fps content,
               timebase should be 1/framerate and timestamp increments should be
               identically 1. */
            videoC.time_base(av_inv_q(frameRate));
            videoC.gop_size(12); /* emit one intra frame every twelve frames at most */
            if (videoQuality >= 0) {
                videoC.flags(videoC.flags() | CODEC_FLAG_QSCALE);
                videoC.global_quality((int)Math.round(FF_QP2LAMBDA * videoQuality));
            }

            if (pixelFormat != AV_PIX_FMT_NONE) {
                videoC.pix_fmt(pixelFormat);
            } else if (videoC.codec_id() == AV_CODEC_ID_RAWVIDEO || videoC.codec_id() == AV_CODEC_ID_PNG ||
                       videoC.codec_id() == AV_CODEC_ID_HUFFYUV  || videoC.codec_id() == AV_CODEC_ID_FFV1) {
                videoC.pix_fmt(AV_PIX_FMT_RGB32);   // appropriate for common lossless formats
            } else {
                videoC.pix_fmt(AV_PIX_FMT_YUV420P); // lossy, but works with about everything
            }

            if (videoC.codec_id() == AV_CODEC_ID_MPEG2VIDEO) {
                /* just for testing, we also add B frames */
                videoC.max_b_frames(2);
            } else if (videoC.codec_id() == AV_CODEC_ID_MPEG1VIDEO) {
                /* Needed to avoid using macroblocks in which some coeffs overflow.
                   This does not happen with normal video, it just happens here as
                   the motion of the chroma plane does not match the luma plane. */
                videoC.mb_decision(2);
            } else if (videoC.codec_id() == AV_CODEC_ID_H263) {
                // H.263 does not support any other resolution than the following
                if (imageWidth <= 128 && imageHeight <= 96) {
                    videoC.width(128).height(96);
                } else if (imageWidth <= 176 && imageHeight <= 144) {
                    videoC.width(176).height(144);
                } else if (imageWidth <= 352 && imageHeight <= 288) {
                    videoC.width(352).height(288);
                } else if (imageWidth <= 704 && imageHeight <= 576) {
                    videoC.width(704).height(576);
                } else {
                    videoC.width(1408).height(1152);
                }
            } else if (videoC.codec_id() == AV_CODEC_ID_H264) {
                // default to constrained baseline to produce content that plays back on anything,
                // without any significant tradeoffs for most use cases
                videoC.profile(AVCodecContext.FF_PROFILE_H264_CONSTRAINED_BASELINE);
            }

            // some formats want stream headers to be separate
            if ((oformat.flags() & AVFMT_GLOBALHEADER) != 0) {
                videoC.flags(videoC.flags() | CODEC_FLAG_GLOBAL_HEADER);
            }

            if ((video_codec.capabilities() & CODEC_CAP_EXPERIMENTAL) != 0) {
                videoC.strict_std_compliance(AVCodecContext.FF_COMPLIANCE_EXPERIMENTAL);
            }
        }

        /*
         * add an audio output stream
         */
        if (audioChannels > 0 && audioBitrate > 0 && sampleRate > 0) {
            if (audioCodec != AV_CODEC_ID_NONE) {
                oformat.audio_codec(audioCodec);
            } else if ("flv".equals(formatName) || "mp4".equals(formatName) || "3gp".equals(formatName)) {
                oformat.audio_codec(AV_CODEC_ID_AAC);
            } else if ("avi".equals(formatName)) {
                oformat.audio_codec(AV_CODEC_ID_PCM_S16LE);
            }

            /* find the audio encoder */
            if ((audio_codec = avcodec_find_encoder_by_name(audioCodecName)) == null &&
                (audio_codec = avcodec_find_encoder(oformat.audio_codec())) == null) {
                release();
                throw new Exception("avcodec_find_encoder() error: Audio codec not found.");
            }

            if ((audioSt = avformat_new_stream(oc, audio_codec)) == null) {
                release();
                throw new Exception("avformat_new_stream() error: Could not allocate audio stream.");
            }
            audioC = audioSt.codec();
            audioC.codec_id(oformat.audio_codec());
            audioC.codec_type(AVMEDIA_TYPE_AUDIO);

            /* put sample parameters */
            audioC.bit_rate(audioBitrate);
            audioC.sample_rate(sampleRate);
            audioC.channels(audioChannels);
            audioC.channel_layout(av_get_default_channel_layout(audioChannels));
            if (sampleFormat != AV_SAMPLE_FMT_NONE) {
                audioC.sample_fmt(sampleFormat);
            } else if (audioC.codec_id() == AV_CODEC_ID_AAC &&
                    (audio_codec.capabilities() & CODEC_CAP_EXPERIMENTAL) != 0) {
                audioC.sample_fmt(AV_SAMPLE_FMT_FLTP);
            } else {
                audioC.sample_fmt(AV_SAMPLE_FMT_S16);
            }
            audioC.time_base().num(1).den(sampleRate);
            switch (audioC.sample_fmt()) {
                case AV_SAMPLE_FMT_U8:
                case AV_SAMPLE_FMT_U8P:  audioC.bits_per_raw_sample(8);  break;
                case AV_SAMPLE_FMT_S16:
                case AV_SAMPLE_FMT_S16P: audioC.bits_per_raw_sample(16); break;
                case AV_SAMPLE_FMT_S32:
                case AV_SAMPLE_FMT_S32P: audioC.bits_per_raw_sample(32); break;
                case AV_SAMPLE_FMT_FLT:
                case AV_SAMPLE_FMT_FLTP: audioC.bits_per_raw_sample(32); break;
                case AV_SAMPLE_FMT_DBL:
                case AV_SAMPLE_FMT_DBLP: audioC.bits_per_raw_sample(64); break;
                default: assert false;
            }
            if (audioQuality >= 0) {
                audioC.flags(audioC.flags() | CODEC_FLAG_QSCALE);
                audioC.global_quality((int)Math.round(FF_QP2LAMBDA * audioQuality));
            }

            // some formats want stream headers to be separate
            if ((oformat.flags() & AVFMT_GLOBALHEADER) != 0) {
                audioC.flags(audioC.flags() | CODEC_FLAG_GLOBAL_HEADER);
            }

            if ((audio_codec.capabilities() & CODEC_CAP_EXPERIMENTAL) != 0) {
                audioC.strict_std_compliance(AVCodecContext.FF_COMPLIANCE_EXPERIMENTAL);
            }
        }

        av_dump_format(oc, 0, filename, 1);

        /* now that all the parameters are set, we can open the audio and
           video codecs and allocate the necessary encode buffers */
        int ret;
        if (videoSt != null) {
            AVDictionary options = new AVDictionary(null);
            if (videoQuality >= 0) {
                av_dict_set(options, "crf", "" + videoQuality, 0);
            }
            for (Entry<String, String> e : videoOptions.entrySet()) {
                av_dict_set(options, e.getKey(), e.getValue(), 0);
            }
            /* open the codec */
            if ((ret = avcodec_open2(videoC, video_codec, options)) < 0) {
                release();
                throw new Exception("avcodec_open2() error " + ret + ": Could not open video codec.");
            }
            av_dict_free(options);

            videoOutbuf = null;
            if ((oformat.flags() & AVFMT_RAWPICTURE) == 0) {
                /* allocate output buffer */
                /* XXX: API change will be done */
                /* buffers passed into lav* can be allocated any way you prefer,
                   as long as they're aligned enough for the architecture, and
                   they're freed appropriately (such as using av_free for buffers
                   allocated with av_malloc) */
                videoOutbufSize = Math.max(256 * 1024, 8 * videoC.width() * videoC.height()); // a la ffmpeg.c
                videoOutbuf = new BytePointer(av_malloc(videoOutbufSize));
            }

            /* allocate the encoded raw picture */
            if ((picture = avcodec_alloc_frame()) == null) {
                release();
                throw new Exception("avcodec_alloc_frame() error: Could not allocate picture.");
            }
            picture.pts(0); // magic required by libx264

            int size = avpicture_get_size(videoC.pix_fmt(), videoC.width(), videoC.height());
            if ((pictureBuf = new BytePointer(av_malloc(size))).isNull()) {
                release();
                throw new Exception("av_malloc() error: Could not allocate picture buffer.");
            }

            /* if the output format is not equal to the image format, then a temporary
               picture is needed too. It is then converted to the required output format */
            if ((tmpPicture = avcodec_alloc_frame()) == null) {
                release();
                throw new Exception("avcodec_alloc_frame() error: Could not allocate temporary picture.");
            }
        }

        if (audioSt != null) {
            AVDictionary options = new AVDictionary(null);
            if (audioQuality >= 0) {
                av_dict_set(options, "crf", "" + audioQuality, 0);
            }
            for (Entry<String, String> e : audioOptions.entrySet()) {
                av_dict_set(options, e.getKey(), e.getValue(), 0);
            }
            /* open the codec */
            if ((ret = avcodec_open2(audioC, audio_codec, options)) < 0) {
                release();
                throw new Exception("avcodec_open2() error " + ret + ": Could not open audio codec.");
            }
            av_dict_free(options);

            audioOutbufSize = 256 * 1024;
            audioOutbuf = new BytePointer(av_malloc(audioOutbufSize));

            /* ugly hack for PCM codecs (will be removed ASAP with new PCM
               support to compute the input frame size in samples */
            if (audioC.frame_size() <= 1) {
                audioOutbufSize = FF_MIN_BUFFER_SIZE;
                audioInputFrameSize = audioOutbufSize / audioC.channels();
                switch (audioC.codec_id()) {
                    case AV_CODEC_ID_PCM_S16LE:
                    case AV_CODEC_ID_PCM_S16BE:
                    case AV_CODEC_ID_PCM_U16LE:
                    case AV_CODEC_ID_PCM_U16BE:
                        audioInputFrameSize >>= 1;
                        break;
                    default:
                        break;
                }
            } else {
                audioInputFrameSize = audioC.frame_size();
            }
            //int bufferSize = audio_input_frame_size * audio_c.bits_per_raw_sample()/8 * audio_c.channels();
            int planes = av_sample_fmt_is_planar(audioC.sample_fmt()) != 0 ? (int) audioC.channels() : 1;
            int dataSize = av_samples_get_buffer_size((IntPointer)null, audioC.channels(),
                    audioInputFrameSize, audioC.sample_fmt(), 1) / planes;
            samplesOut = new BytePointer[planes];
            for (int i = 0; i < samplesOut.length; i++) {
                samplesOut[i] = new BytePointer(av_malloc(dataSize)).capacity(dataSize);
            }
            samplesIn = new Pointer[AVFrame.AV_NUM_DATA_POINTERS];
            samplesInPtr = new PointerPointer(AVFrame.AV_NUM_DATA_POINTERS);
            samplesOutPtr = new PointerPointer(AVFrame.AV_NUM_DATA_POINTERS);

            /* allocate the audio frame */
            if ((frame = avcodec_alloc_frame()) == null) {
                release();
                throw new Exception("avcodec_alloc_frame() error: Could not allocate audio frame.");
            }
        }

        /* open the output file, if needed */
        if ((oformat.flags() & AVFMT_NOFILE) == 0) {
            AVIOContext pb = new AVIOContext(null);
            if ((ret = avio_open(pb, filename, AVIO_FLAG_WRITE)) < 0) {
                release();
                throw new Exception("avio_open error() error " + ret + ": Could not open '" + filename + "'");
            }
            oc.pb(pb);
        }

        /* write the stream header, if any */
        avformat_write_header(oc, (PointerPointer)null);
    }

    public void stop() throws Exception {
        if (oc != null) {
            try {
                /* flush all the buffers */
                while (videoSt != null && record((IplImage)null, AV_PIX_FMT_NONE));
                while (audioSt != null && record((AVFrame)null));

                if (interleaved && videoSt != null && audioSt != null) {
                    av_interleaved_write_frame(oc, null);
                } else {
                    av_write_frame(oc, null);
                }

                /* write the trailer, if any */
                av_write_trailer(oc);
            } finally {
                release();
            }
        }
    }

    public boolean record(IplImage image) throws Exception {
        return record(image, AV_PIX_FMT_NONE);
    }
    
    public boolean record(IplImage image, int pixelFormat) throws Exception {
        if (videoSt == null) {
            throw new Exception("No video output stream (Is imageWidth > 0 && imageHeight > 0 and has start() been called?)");
        }

        if (image == null) {
            /* no more frame to compress. The codec has a latency of a few
               frames if using B frames, so we get the last frames by
               passing the same picture again */
        } else {
            int width = image.width();
            int step = image.widthStep();

            if (pixelFormat == AV_PIX_FMT_NONE) {
                int depth = image.depth();
                int channels = image.nChannels();
                if ((depth == IPL_DEPTH_8U || depth == IPL_DEPTH_8S) && channels == 3) {
                    pixelFormat = AV_PIX_FMT_BGR24;
                } else if ((depth == IPL_DEPTH_8U || depth == IPL_DEPTH_8S) && channels == 1) {
                    pixelFormat = AV_PIX_FMT_GRAY8;
                } else if ((depth == IPL_DEPTH_16U || depth == IPL_DEPTH_16S) && channels == 1) {
                    pixelFormat = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN) ?
                            AV_PIX_FMT_GRAY16BE : AV_PIX_FMT_GRAY16LE;
                } else if ((depth == IPL_DEPTH_8U || depth == IPL_DEPTH_8S) && channels == 4) {
                    pixelFormat = AV_PIX_FMT_RGBA;
                } else if ((depth == IPL_DEPTH_8U || depth == IPL_DEPTH_8S) && channels == 2) {
                    pixelFormat = AV_PIX_FMT_NV21; // Android's camera capture format
                    step = width;
                } else {
                    throw new Exception("Could not guess pixel format of image: depth=" + depth + ", channels=" + channels);
                }
            }

            int height = image.height();
            BytePointer data = image.imageData();
            if (videoC.pix_fmt() != pixelFormat || videoC.width() != width || videoC.height() != height) {
                /* convert to the codec pixel format if needed */
                imgConvertCtx = sws_getCachedContext(imgConvertCtx,  videoC.width(), videoC.height(), pixelFormat,
                        videoC.width(), videoC.height(), videoC.pix_fmt(), SWS_BILINEAR,
                        null, null, (DoublePointer)null);
                if (imgConvertCtx == null) {
                    throw new Exception("sws_getCachedContext() error: Cannot initialize the conversion context.");
                }
                avpicture_fill(new AVPicture(tmpPicture), data, pixelFormat, width, height);
                avpicture_fill(new AVPicture(picture), pictureBuf, videoC.pix_fmt(), videoC.width(), videoC.height());
                tmpPicture.linesize(0, step);
                sws_scale(imgConvertCtx, new PointerPointer(tmpPicture), tmpPicture.linesize(),
                          0, height, new PointerPointer(picture), picture.linesize());
            } else {
                avpicture_fill(new AVPicture(picture), data, pixelFormat, width, height);
                picture.linesize(0, step);
            }
        }

        int ret;
        if ((oformat.flags() & AVFMT_RAWPICTURE) != 0) {
            if (image == null) {
                return false;
            }
            /* raw video case. The API may change slightly in the future for that? */
            av_init_packet(videoPkt);
            videoPkt.flags(videoPkt.flags() | AV_PKT_FLAG_KEY);
            videoPkt.stream_index(videoSt.index());
            videoPkt.data(new BytePointer(picture));
            videoPkt.size(Loader.sizeof(AVPicture.class));
        } else {
            /* encode the image */
            av_init_packet(videoPkt);
            videoPkt.data(videoOutbuf);
            videoPkt.size(videoOutbufSize);
            picture.quality(videoC.global_quality());
            if ((ret = avcodec_encode_video2(videoC, videoPkt, image == null ? null : picture, gotVideoPacket)) < 0) {
                throw new Exception("avcodec_encode_video2() error " + ret + ": Could not encode video packet.");
            }
            picture.pts(picture.pts() + 1); // magic required by libx264

            /* if zero size, it means the image was buffered */
            if (gotVideoPacket[0] != 0) {
                if (videoPkt.pts() != AV_NOPTS_VALUE) {
                    videoPkt.pts(av_rescale_q(videoPkt.pts(), videoC.time_base(), videoSt.time_base()));
                }
                if (videoPkt.dts() != AV_NOPTS_VALUE) {
                    videoPkt.dts(av_rescale_q(videoPkt.dts(), videoC.time_base(), videoSt.time_base()));
                }
                videoPkt.stream_index(videoSt.index());
            } else {
                return false;
            }
        }

        synchronized (oc) {
            /* write the compressed frame in the media file */
            if (interleaved && audioSt != null) {
                if ((ret = av_interleaved_write_frame(oc, videoPkt)) < 0) {
                    throw new Exception("av_interleaved_write_frame() error " + ret + " while writing interleaved video frame.");
                }
            } else {
                if ((ret = av_write_frame(oc, videoPkt)) < 0) {
                    throw new Exception("av_write_frame() error " + ret + " while writing video frame.");
                }
            }
        }
        return picture.key_frame() != 0;
    }

    @Override public boolean record(int sampleRate, Buffer ... samples) throws Exception {
        if (audioSt == null) {
            throw new Exception("No audio output stream (Is audioChannels > 0 and has start() been called?)");
        }

        int inputSize = samples[0].limit() - samples[0].position();
        int inputFormat;
        int inputDepth;
        if (sampleRate <= 0) {
            sampleRate = audioC.sample_rate();
        }
        if (samples[0] instanceof ByteBuffer) {
            inputFormat = samples.length > 1 ? AV_SAMPLE_FMT_U8P : AV_SAMPLE_FMT_U8;
            inputDepth = 1;
            for (int i = 0; i < samples.length; i++) {
                ByteBuffer b = (ByteBuffer)samples[i];
                if (samplesIn[i] instanceof BytePointer && samplesIn[i].capacity() >= inputSize && b.hasArray()) {
                    ((BytePointer) samplesIn[i]).position(0).put(b.array(), b.position(), inputSize);
                } else {
                    samplesIn[i] = new BytePointer(b);
                }
            }
        } else if (samples[0] instanceof ShortBuffer) {
            inputFormat = samples.length > 1 ? AV_SAMPLE_FMT_S16P : AV_SAMPLE_FMT_S16;
            inputDepth = 2;
            for (int i = 0; i < samples.length; i++) {
                ShortBuffer b = (ShortBuffer)samples[i];
                if (samplesIn[i] instanceof ShortPointer && samplesIn[i].capacity() >= inputSize && b.hasArray()) {
                    ((ShortPointer) samplesIn[i]).position(0).put(b.array(), samples[i].position(), inputSize);
                } else {
                    samplesIn[i] = new ShortPointer(b);
                }
            }
        } else if (samples[0] instanceof IntBuffer) {
            inputFormat = samples.length > 1 ? AV_SAMPLE_FMT_S32P : AV_SAMPLE_FMT_S32;
            inputDepth = 4;
            for (int i = 0; i < samples.length; i++) {
                IntBuffer b = (IntBuffer)samples[i];
                if (samplesIn[i] instanceof IntPointer && samplesIn[i].capacity() >= inputSize && b.hasArray()) {
                    ((IntPointer) samplesIn[i]).position(0).put(b.array(), samples[i].position(), inputSize);
                } else {
                    samplesIn[i] = new IntPointer(b);
                }
            }
        } else if (samples[0] instanceof FloatBuffer) {
            inputFormat = samples.length > 1 ? AV_SAMPLE_FMT_FLTP : AV_SAMPLE_FMT_FLT;
            inputDepth = 4;
            for (int i = 0; i < samples.length; i++) {
                FloatBuffer b = (FloatBuffer)samples[i];
                if (samplesIn[i] instanceof FloatPointer && samplesIn[i].capacity() >= inputSize && b.hasArray()) {
                    ((FloatPointer) samplesIn[i]).position(0).put(b.array(), b.position(), inputSize);
                } else {
                    samplesIn[i] = new FloatPointer(b);
                }
            }
        } else if (samples[0] instanceof DoubleBuffer) {
            inputFormat = samples.length > 1 ? AV_SAMPLE_FMT_DBLP : AV_SAMPLE_FMT_DBL;
            inputDepth = 8;
            for (int i = 0; i < samples.length; i++) {
                DoubleBuffer b = (DoubleBuffer)samples[i];
                if (samplesIn[i] instanceof DoublePointer && samplesIn[i].capacity() >= inputSize && b.hasArray()) {
                    ((DoublePointer) samplesIn[i]).position(0).put(b.array(), b.position(), inputSize);
                } else {
                    samplesIn[i] = new DoublePointer(b);
                }
            }
        } else {
            throw new Exception("Audio samples Buffer has unsupported type: " + samples);
        }

        int ret;
        int outputFormat = audioC.sample_fmt();
        if (samplesConvertCtx == null) {
            samplesConvertCtx = swr_alloc_set_opts(null,
                    audioC.channel_layout(), outputFormat, audioC.sample_rate(),
                    audioC.channel_layout(), inputFormat, sampleRate, 0, null);
            if (samplesConvertCtx == null) {
                throw new Exception("swr_alloc_set_opts() error: Cannot allocate the conversion context.");
            } else if ((ret = swr_init(samplesConvertCtx)) < 0) {
                throw new Exception("swr_init() error " + ret + ": Cannot initialize the conversion context.");
            }
        }

        for (int i = 0; i < samples.length; i++) {
            samplesIn[i].position(samplesIn[i].position() * inputDepth).
                    limit((samplesIn[i].position() + inputSize) * inputDepth);
        }

        int outputChannels = samplesOut.length > 1 ? 1 : audioChannels;
        int inputChannels = samples.length > 1 ? 1 : audioChannels;
        int outputDepth = av_get_bytes_per_sample(outputFormat);
        while (true) {
            int inputCount = (samplesIn[0].limit() - samplesIn[0].position()) / (inputChannels * inputDepth);
            int outputCount = (samplesOut[0].limit() - samplesOut[0].position()) / (outputChannels * outputDepth);
            inputCount = Math.min(inputCount, 2 * (outputCount * sampleRate) / audioC.sample_rate());
            for (int i = 0; i < samples.length; i++) {
                samplesInPtr.put(i, samplesIn[i]);
            }
            for (int i = 0; i < samplesOut.length; i++) {
                samplesOutPtr.put(i, samplesOut[i]);
            }
            if ((ret = swr_convert(samplesConvertCtx, samplesOutPtr, outputCount, samplesInPtr, inputCount)) < 0) {
                throw new Exception("swr_convert() error " + ret + ": Cannot convert audio samples.");
            } else if (ret == 0) {
                break;
            }
            for (int i = 0; i < samples.length; i++) {
                samplesIn[i].position(samplesIn[i].position() + inputCount * inputChannels * inputDepth);
            }
            for (int i = 0; i < samplesOut.length; i++) {
                samplesOut[i].position(samplesOut[i].position() + ret * outputChannels * outputDepth);
            }

            if (samplesOut[0].position() >= samplesOut[0].limit()) {
                frame.nb_samples(audioInputFrameSize);
                avcodec_fill_audio_frame(frame, audioC.channels(), outputFormat, samplesOut[0], samplesOut[0].limit(), 0);
                for (int i = 0; i < samplesOut.length; i++) {
                    frame.data(i, samplesOut[i].position(0));
                    frame.linesize(i, samplesOut[i].limit());
                }
                frame.quality(audioC.global_quality());
                record(frame);
            }
        }
        return frame.key_frame() != 0;
    }

    boolean record(AVFrame frame) throws Exception {
        int ret;

        av_init_packet(audioPkt);
        audioPkt.data(audioOutbuf);
        audioPkt.size(audioOutbufSize);
        if ((ret = avcodec_encode_audio2(audioC, audioPkt, frame, gotAudioPacket)) < 0) {
            throw new Exception("avcodec_encode_audio2() error " + ret + ": Could not encode audio packet.");
        }
        if (gotAudioPacket[0] != 0) {
            if (audioPkt.pts() != AV_NOPTS_VALUE) {
                audioPkt.pts(av_rescale_q(audioPkt.pts(), audioC.time_base(), audioC.time_base()));
            }
            if (audioPkt.dts() != AV_NOPTS_VALUE) {
                audioPkt.dts(av_rescale_q(audioPkt.dts(), audioC.time_base(), audioC.time_base()));
            }
            audioPkt.flags(audioPkt.flags() | AV_PKT_FLAG_KEY);
            audioPkt.stream_index(audioSt.index());
        } else {
            return false;
        }

        /* write the compressed frame in the media file */
        synchronized (oc) {
            if (interleaved && videoSt != null) {
                if ((ret = av_interleaved_write_frame(oc, audioPkt)) < 0) {
                    throw new Exception("av_interleaved_write_frame() error " + ret + " while writing interleaved audio frame.");
                }
            } else {
                if ((ret = av_write_frame(oc, audioPkt)) < 0) {
                    throw new Exception("av_write_frame() error " + ret + " while writing audio frame.");
                }
            }
        }
        return true;
    }
}
