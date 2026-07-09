package dev.luna5ama.trollhack.modules.impl.client

import dev.luna5ama.trollhack.utils.Displayable
import dev.luna5ama.trollhack.utils.ResourceHelper
import dev.luna5ama.trollhack.graphics.color.ColorRGBA
import dev.luna5ama.trollhack.graphics.texture.MipmapTexture
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

enum class AnimeType(override val displayName: CharSequence, val fileName: String, val color: ColorRGBA) : Displayable {
    HITORI_GOTOH("Hitori Gotoh", "HitoriGotoh", ColorRGBA(255, 76, 135)),
    IJICHI_NIJIKA("Ijichi Nijika", "IjichiNijika", ColorRGBA(244, 210, 98)),
    YAMADA_RYO("Yamada Ryo", "YamadaRyo", ColorRGBA(58, 99, 167)),
    KITA_IKUYO("Kita Ikuyo", "KitaIkuyo", ColorRGBA(221, 80, 89)),
    ISERI_NINA("Iseri Nina", "IseriNina", ColorRGBA(217, 14, 44)),
    KAWARAGI_MOMOKA("Kawaragi Momoka", "KawaragiMomoka", ColorRGBA(133, 201, 220)),
    AWA_SUBARU("Awa Subaru", "AwaSubaru", ColorRGBA(118, 189, 83)),
    RUPA("Rupa", "Rupa", ColorRGBA(238, 218, 1)),
    EBIZUKA_TOMO("Ebizuka Tomo", "EbizukaTomo", ColorRGBA(227, 77, 141)),
    LYCORIS_PARTNER("Lycoris Partner", "LycorisPartner", ColorRGBA(213, 60, 69)),
    NISHIKIGI_CHISATO("Nishikigi Chisato", "NishikigiChisato", ColorRGBA(213, 60, 69)),
    NISHIKIGI_CHISATO2("Nishikigi Chisato (2)", "NishikigiChisato2", ColorRGBA(213, 60, 69)),
    INOE_TAKINA("Inoe Takina", "InoeTakina", ColorRGBA(45, 51, 69)),
    AKIYAMA_MIO("Akiyama Mio", "AkiyamaMio", ColorRGBA(43, 52, 67)),
    NAKANO_AZUSA("Nakano Azusa", "NakanoAzusa", ColorRGBA(43, 52, 67)),
    AZUMA_SEREN("Azuma Seren", "AzumaSeren", ColorRGBA(113, 139, 175)),
    MASHIRO_KANON("Mashiro Kanon", "MashiroKanon", ColorRGBA(252, 218, 231)),
    ACE_TAFFY("Ace Taffy", "AceTaffy", ColorRGBA(233, 193, 202));

    val texture = MipmapTexture(loadAnimeImage(fileName))
}

private fun loadAnimeImage(fileName: String): BufferedImage {
    return readAnimeImage("/assets/trollhack/background/$fileName.png")
        ?: readAnimeImage("/assets/trollhack/background/HitoriGotoh.png")
        ?: BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
}

private fun readAnimeImage(path: String): BufferedImage? {
    return ResourceHelper.getResourceStream(path)?.use(ImageIO::read)
}
