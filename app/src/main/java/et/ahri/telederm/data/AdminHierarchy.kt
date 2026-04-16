package et.ahri.telederm.data

object AdminHierarchy {
    val federal = listOf("Federal Democratic Republic of Ethiopia")

    val regions = listOf(
        "Addis Ababa", "Afar", "Amhara", "Benishangul-Gumuz", "Dire Dawa",
        "Gambela", "Harari", "Oromia", "Sidama", "Somali", "SNNPR",
        "Southwest Ethiopia Peoples’ Region", "Tigray"
    ).sorted()

    val zonesMap = mapOf(
        "Afar" to listOf("Zone 1", "Zone 2", "Zone 3", "Zone 4", "Zone 5"),
        "Amhara" to listOf(
            "North Gondar", "South Gondar", "North Wollo", "South Wollo",
            "North Shewa", "South Shewa", "East Gojjam", "West Gojjam",
            "Agew Awi", "Wag Hemra", "Oromia Zone"
        ),
        "Benishangul-Gumuz" to listOf("Asosa", "Kamashi", "Metekel"),
        "Gambela" to listOf("Anuak", "Nuer", "Majang"),
        "Harari" to listOf("Harari"),
        "Oromia" to listOf(
            "Arsi", "Bale", "Borena", "East Hararghe", "West Hararghe", "East Shewa",
            "West Shewa", "North Shewa", "East Wollega", "West Wollega",
            "Kelem Wollega", "Horo Gudru Wollega", "Jimma", "Illubabor", "Guji",
            "West Guji", "Buno Bedele", "West Arsi", "Southwest Shewa"
        ),
        "Sidama" to listOf("Sidama Zone"),
        "Somali" to listOf(
            "Sitti",
            "Fafan",
            "Jarar",
            "Nogob",
            "Dollo",
            "Korahe",
            "Shabelle",
            "Afder",
            "Liban"
        ),
        "SNNPR" to listOf(
            "Wolayita", "Hadiya", "Gurage", "Gamo", "Gofa", "Kembata Tembaro",
            "Silte", "Dawro", "Gedeo", "Kafa", "Sheka", "Bench Maji", "South Omo"
        ),
        "Southwest Ethiopia Peoples’ Region" to listOf(
            "Bench Sheko",
            "Dawro",
            "Kafa",
            "Sheka",
            "Konta Special"
        ),
        "Tigray" to listOf(
            "Central Tigray", "Eastern Tigray", "Southern Tigray",
            "Northwestern Tigray", "Western Tigray", "Mekelle"
        ),
        "Addis Ababa" to listOf("Addis Ababa"),
        "Dire Dawa" to listOf("Dire Dawa")
    ).mapValues { it.value.sorted() }

    val woredasMap = mapOf(
        // Amhara
        "North Gondar" to listOf(
            "Debark",
            "Dabat",
            "Adi Arkay",
            "Jan Amora",
            "Wegera",
            "Lay Armachiho",
            "Chilga",
            "Metema",
            "Quara",
            "Takusa",
            "Alefa",
            "Belesa",
            "Tegede"
        ),
        "South Gondar" to listOf(
            "Debre Tabor",
            "Farta",
            "Estie",
            "Fogera",
            "Dera",
            "Libo Kemkem",
            "Simada",
            "Tach Gayint",
            "Lay Gayint"
        ),
        "North Wollo" to listOf(
            "Woldiya",
            "Kobo",
            "Habru",
            "Guba Lafto",
            "Meket",
            "Bugna",
            "Lasta",
            "Dawunt",
            "Delanta"
        ),
        "South Wollo" to listOf(
            "Dessie Zuria",
            "Kutaber",
            "Tehuledere",
            "Tenta",
            "Were Ilu",
            "Ambassel",
            "Sayint",
            "Legambo",
            "Kelala",
            "Jama",
            "Werebabo"
        ),
        "North Shewa" to listOf(
            "Debre Berhan",
            "Ankober",
            "Tarmaber",
            "Basona Werana",
            "Angolalla Tera",
            "Kewet",
            "Efratana Gidim",
            "Menz Gera Midir",
            "Menz Lalo Midir",
            "Menz Mama Midir"
        ),
        "South Shewa" to listOf(
            "Debre Sina",
            "Mehal Meda",
            "Ensaro",
            "Moretna Jiru",
            "Berehet",
            "Siyadebrina Wayu",
            "Kembibit",
            "Hagere Mariam"
        ),
        "East Gojjam" to listOf(
            "Debre Markos",
            "Hulet Ej Enese",
            "Enemay",
            "Shebel Berenta",
            "Dejen",
            "Awabel",
            "Baso Liben",
            "Gozamin",
            "Machakel",
            "Sinan"
        ),
        "West Gojjam" to listOf(
            "Finote Selam",
            "Jabi Tehnan",
            "Bure",
            "Sekela",
            "Dangila",
            "Dembecha",
            "Quarit",
            "Yilmana Densa"
        ),
        "Agew Awi" to listOf("Dangila", "Guangua", "Ankesha", "Banja Shekudad", "Faggeta Lekoma"),
        "Wag Hemra" to listOf("Sekota", "Zikuala", "Dehana", "Abergele", "Sahila"),
        "Oromia Zone" to listOf("Kemise", "Bati", "Artuma Fursi", "Jille Timuga", "Dawa Chefa"),

        // Afar
        "Zone 1" to listOf(
            "Asayita",
            "Afambo",
            "Dubti",
            "Mille",
            "Chifra",
            "Gewane",
            "Amibara",
            "Bure Mudaytu",
            "Awash Fentale"
        ),
        "Zone 2" to listOf("Erebti", "Abala", "Berahile", "Afdera", "Dalol", "Kuneba", "Megale"),
        "Zone 3" to listOf("Awash", "Amibara", "Gewane", "Bure Mudaytu", "Awash Fentale"),
        "Zone 4" to listOf("Gulina", "Yalo", "Teru", "Aura", "Ewa"),
        "Zone 5" to listOf("Dalifage", "Dewe", "Hadele Ele", "Semurobi Gele’alo"),

        // Benishangul-Gumuz
        "Asosa" to listOf("Asosa", "Bambasi", "Menge", "Sherkole", "Kurmuk", "Homosha"),
        "Kamashi" to listOf("Kamashi", "Yaso", "Sirba Abay", "Agalo Meti"),
        "Metekel" to listOf("Pawe", "Mandura", "Dibate", "Bulen", "Dangur", "Guba", "Wenbera"),

        // Gambela
        "Anuak" to listOf("Gambela Zuria", "Abobo", "Gog", "Jor", "Itang"),
        "Nuer" to listOf("Akobo", "Jikawo", "Lare", "Makuey"),
        "Majang" to listOf("Godere", "Mengesh"),

        // Harari
        "Harari" to listOf(
            "Sofi",
            "Erer",
            "Dire Teyara",
            "Abadir",
            "Shenkor",
            "Hakim",
            "Jenella",
            "Addis Ketema"
        ),

        // Oromia
        "Arsi" to listOf(
            "Aminya",
            "Aseko",
            "Asella Town",
            "Bale Gasegar",
            "Batu Dugda",
            "Chole",
            "Digelu fi Tijo",
            "Diksis",
            "Dodota",
            "Enkelo Wabe",
            "Gololcha",
            "Guna",
            "Hetosa",
            "Jeju",
            "Lemu fi Bilbilo",
            "Lode Hetosa",
            "Merti",
            "Munesa",
            "Robe",
            "Seru",
            "Sire",
            "Shirka",
            "Sude",
            "Tena",
            "Tiyo"
        ),
        "Bale" to listOf(
            "Agarfa",
            "Berbere",
            "Delo Menna",
            "Dinsho",
            "Gasera",
            "Goba",
            "Goba Town",
            "Goro",
            "Guradamole",
            "Harena Buluk",
            "Meda Welabu",
            "Robe Town",
            "Sinana",
            "Sawena",
            "Rayitu",
            "Legahidha",
            "Gindhir",
            "Dawe Qachan",
            "Dawe Sarar",
            "Gololcha",
            "Gindhir Town"
        ),
        "Borena" to listOf(
            "Dillo",
            "Dire",
            "Gomole",
            "Miyo",
            "Moyale",
            "Teltele",
            "Yabelo",
            "Dubuluq",
            "Elwaye",
            "Yabelo Town",
            "Guchi"
        ),
        "Buno Bedele" to listOf(
            "Bedele",
            "Chora",
            "Dabo",
            "Chawaka",
            "Boracha",
            "Gechi",
            "Dedesa",
            "Dega",
            "Meko",
            "Badele Town",
            "Dabo Hana",
            "Doreni"
        ),
        "East Hararghe" to listOf(
            "Babile",
            "Bedeno",
            "Chinaksen",
            "Dadar",
            "Fedis",
            "Girawa",
            "Gola Oda",
            "Goro Gutu",
            "Gursum",
            "Haro Maya",
            "Jarso",
            "Kersa",
            "Kombolcha",
            "Kurfa Chele",
            "Melka Balo",
            "Meta"
        ),
        "West Hararghe" to listOf(
            "Chiro",
            "Habro",
            "Gemechis",
            "Miesso",
            "Doba",
            "Tulo",
            "Guba Koricha",
            "Mesela",
            "Oda Bultum",
            "Anchar"
        ),
        "East Shewa" to listOf("Adama", "Boset", "Lume", "Dugda", "Bora", "Ada’a", "Fentale"),
        "West Shewa" to listOf(
            "Ambo",
            "Jeldu",
            "Dendi",
            "Toke Kutaye",
            "Ilu Galan",
            "Ejere",
            "Ginde Beret",
            "Midakegn",
            "Tikur Inchini",
            "Wenchi",
            "Nono",
            "Bako Tibe"
        ),
        "North Shewa" to listOf(
            "Fiche",
            "Kuyu",
            "Degem",
            "Warra Jarso",
            "Girar Jarso",
            "Abichu",
            "Aleltu",
            "Berehna Aleltu"
        ),
        "East Wollega" to listOf(
            "Limu Ibantu",
            "Gida Kiremu",
            "Haro Limu",
            "Boneya Bushe",
            "Wayu Tuka",
            "Gudeya Bila",
            "Gobu Seyo",
            "Sibu Sire",
            "Diga",
            "Sasiga",
            "Leka Dulecha",
            "Guto Gida",
            "Jima Arjo",
            "Nunu Kumba",
            "Nekemte"
        ),
        "West Wollega" to listOf(
            "Menesibu",
            "Nejo",
            "Gimbi",
            "Lalo Asabi",
            "Kiltu Kara",
            "Boji",
            "Dirmeji",
            "Guliso",
            "Jarso",
            "Kondala",
            "Boji Chekorsa",
            "Babo Gambel",
            "Yubdo",
            "Genji",
            "Haru",
            "Nole Kaba",
            "Begi",
            "Seyo Nole",
            "Homa",
            "Ayira"
        ),
        "Kelem Wollega" to listOf(
            "Dembi Dollo",
            "Sayo",
            "Yamalogi Welele",
            "Hawa Welele",
            "Anfillo"
        ),
        "Horo Gudru Wollega" to listOf(
            "Shambu",
            "Jarte Jardega",
            "Guduru",
            "Abay Chomen",
            "Jimma Genete",
            "Horro",
            "Amuru"
        ),
        "Jimma" to listOf(
            "Limu Seka",
            "Limu Kosa",
            "Sokoru",
            "Tiro Afeta",
            "Kersa",
            "Mana",
            "Gomma",
            "Gera",
            "Seka Chekorsa",
            "Dedo",
            "Omonada",
            "Sigamo",
            "Setema",
            "Shebe Senbo",
            "Chora Botor",
            "Guma",
            "Agaro Town"
        ),
        "Illubabor" to listOf(
            "Darimu",
            "Alge Sachi",
            "Chora",
            "Dega",
            "Dabo Hana",
            "Gechi",
            "Borecha",
            "Dedesa",
            "Yayu",
            "Metu Zuria",
            "Ale",
            "Bure",
            "Nono Sele",
            "Bicho",
            "Bilo Nopha",
            "Hurumu",
            "Didu",
            "Mako",
            "Huka/Halu",
            "Metu Town"
        ),
        "Guji" to listOf("Negelle", "Adola", "Wadera", "Liben", "Odo Shakiso", "Bore", "Ana Sora"),
        "West Guji" to listOf(
            "Bule Hora",
            "Kercha",
            "Dugda Dawa",
            "Suro Berguda",
            "Hambela Wamena"
        ),
        "West Arsi" to listOf(
            "Shashamane",
            "Arsi Negele",
            "Kofele",
            "Dodola",
            "Adaba",
            "Nensebo",
            "Kore",
            "Shala"
        ),
        "Southwest Shewa" to listOf(
            "Waliso",
            "Becho",
            "Goro",
            "Sebeta Hawas",
            "Ilu",
            "Dawo",
            "Seden Sodo"
        ),

        // Sidama
        "Sidama Zone" to listOf(
            "Hawassa Zuria",
            "Dale",
            "Shebedino",
            "Boricha",
            "Wondo Genet",
            "Aleta Wendo",
            "Hula",
            "Loka Abaya",
            "Bona Zuria",
            "Arbegona",
            "Bursa",
            "Chuko",
            "Dara",
            "Gorche",
            "Malga",
            "Wensho",
            "Yirgalem Town",
            "Hawassa City"
        ),

        // Somali
        "Sitti" to listOf("Shinile", "Erer", "Afdem", "Meiso"),
        "Fafan" to listOf("Jijiga", "Awbare", "Babile", "Gursum"),
        "Jarar" to listOf("Degehabur", "Aware", "Misraq Gashamo"),
        "Nogob" to listOf("Fiq", "Segeg", "Hamero"),
        "Dollo" to listOf("Warder", "Geladin", "Bokh"),
        "Korahe" to listOf("Kebri Dehar", "Shilabo", "Marsin"),
        "Shabelle" to listOf("Gode", "Kelafo", "Mustahil"),
        "Afder" to listOf("Hargele", "Dolobay", "Bare"),
        "Liban" to listOf("Filtu", "Dolo Odo"),

        // SNNPR
        "Wolayita" to listOf(
            "Sodo Zuria",
            "Damot Gale",
            "Damot Sore",
            "Boloso Sore",
            "Humbo",
            "Duguna Fango",
            "Kindo Koysha",
            "Sodo Town"
        ),
        "Hadiya" to listOf(
            "Hosaena",
            "Lemo",
            "Gibe",
            "Soro",
            "Misha",
            "Mirab Badawacho",
            "Misraq Badawacho"
        ),
        "Gurage" to listOf(
            "Wolkite",
            "Ezha",
            "Cheha",
            "Abeshge",
            "Kebena",
            "Muhirna Aklil",
            "Meskan",
            "Sodo",
            "Gumer"
        ),
        "Gamo" to listOf("Arba Minch Zuria", "Bonke", "Chencha", "Dita", "Mirab Abaya", "Boreda"),
        "Gofa" to listOf("Sawla Town", "Zala", "Demba Gofa", "Malo Koza"),
        "Kembata Tembaro" to listOf(
            "Kedida Gamela",
            "Angacha",
            "Doyogena",
            "Hadero Tunto",
            "Kacha Bira"
        ),
        "Silte" to listOf("Silte", "Sankura", "Hulbareg", "Azernet Berbere"),
        "Dawro" to listOf("Tocha", "Mareka", "Loma", "Isara", "Dawro Zuria", "Tarcha"),
        "Gedeo" to listOf("Dilla Zuria", "Gedeb", "Yirgachefe", "Kochere", "Bule"),
        "Kafa" to listOf("Bonga", "Gimbo", "Decha", "Chena", "Gesha", "Sayilem"),
        "Sheka" to listOf("Masha", "Yeki", "Anderacha"),
        "Bench Maji" to listOf("Bench", "Sheko", "Mizan Aman Town"),
        "South Omo" to listOf(
            "Jinka Town",
            "Hamer",
            "Dassenech",
            "Bena Tsemay",
            "Nyangatom",
            "Selamago"
        ),

        // Southwest Ethiopia Peoples’ Region
        "Bench Sheko" to listOf("Bench", "Sheko", "Mizan Aman Town"),
        "Konta Special" to listOf("Konta"),

        // Tigray
        "Central Tigray" to listOf(
            "Adwa",
            "Axum",
            "Laelay Maychew",
            "Tahtay Maychew",
            "Ahferom",
            "Mereb Leke"
        ),
        "Eastern Tigray" to listOf(
            "Adigrat",
            "Atsbi",
            "Hawzen",
            "Saesi Tsaeda Emba",
            "Ganta Afeshum"
        ),
        "Southern Tigray" to listOf("Alamata", "Raya Azebo", "Ofla", "Endamehoni", "Korem Town"),
        "Northwestern Tigray" to listOf(
            "Shire",
            "Tahtay Adiyabo",
            "Asgede Tsimbla",
            "Laelay Adiyabo"
        ),
        "Western Tigray" to listOf("Humera", "Wolqayt", "Tsegede"),
        "Mekelle" to listOf("Mekelle City"),

        // Cities
        "Addis Ababa" to listOf(
            "Addis Ketema",
            "Akaki Kality",
            "Arada",
            "Bole",
            "Gullele",
            "Kirkos",
            "Kolfe Keranio",
            "Lideta",
            "Nifas Silk-Lafto",
            "Yeka"
        ),
        "Dire Dawa" to listOf("Dire Dawa")
    ).mapValues { it.value.sorted() }
}
