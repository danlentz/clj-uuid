(ns clj-uuid.v3-test
  (:require [clj-uuid.core  :as uuid]
            [clojure.test   :refer :all]))


(deftest check-v3-special-cases
  (testing "v3 special case correctness..."
    (is (=
          (uuid/v3 uuid/+null+ "")
          #uuid "4ae71336-e44b-39bf-b9d2-752e234818a5"))
    (is (=
          (uuid/v3 uuid/+namespace-x500+ "")
          #uuid "7AAF118C-F174-3EBA-9EC5-680CD791A020"))
    (is (=
          (uuid/v3 uuid/+namespace-oid+ "")
          #uuid "596B79DC-00DD-3991-A72F-D3696C38C64F"))
    (is (=
          (uuid/v3 uuid/+namespace-dns+ "")
          #uuid "C87EE674-4DDC-3EFE-A74E-DFE25DA5D7B3"))
    (is (=
          (uuid/v3 uuid/+namespace-url+ "")
          #uuid "14CDB9B4-DE01-3FAA-AFF5-65BC2F771745"))))


(def +v3-null-ns-cases+
  '((" !\"#$%&'()*+,-./0123456789" #uuid "84527A03-63CA-381A-8AFB-CF4244EF61FE")
    ("!\"#$%&'()*+,-./0123456789:" #uuid "50D816D1-EEBA-3CA5-9C84-2C1C81EA53EF")
    ("\"#$%&'()*+,-./0123456789:;" #uuid "A52D645C-B81C-3DD9-8AC7-E3B9E27B7399")
    ("#$%&'()*+,-./0123456789:;<" #uuid "2F73C64B-D58B-3714-93A0-5208599BDD84")
    ("$%&'()*+,-./0123456789:;<=" #uuid "3624A1E8-15D2-35CD-90E3-A482CF6ED426")
    ("%&'()*+,-./0123456789:;<=>" #uuid "B75CCFA3-6E9E-301A-A7E7-80DF0EFB5E11")
    ("&'()*+,-./0123456789:;<=>?" #uuid "CC1CA3BD-9FEA-348E-8133-857306B90520")
    ("'()*+,-./0123456789:;<=>?@" #uuid "A7C27CC9-4F2D-3155-88AF-2E10A000A929")
    ("()*+,-./0123456789:;<=>?@A" #uuid "A9B01A6B-6ECB-3754-A397-7C92BCB34233")
    (")*+,-./0123456789:;<=>?@AB" #uuid "31FB5B5C-0CAC-3C90-B6CD-9443645F4FD7")
    ("*+,-./0123456789:;<=>?@ABC" #uuid "47F741D2-E7E8-3D96-86D7-C955CDE87E0D")
    ("+,-./0123456789:;<=>?@ABCD" #uuid "8EFFE6B8-1B60-35C7-BE1E-6D67A3FF5D55")
    (",-./0123456789:;<=>?@ABCDE" #uuid "6FE5CED9-30CF-3D2A-92A0-9D0AFB1A857C")
    ("-./0123456789:;<=>?@ABCDEF" #uuid "F9AF2E93-0DF6-3C2E-BEE9-137ED731385D")
    ("./0123456789:;<=>?@ABCDEFG" #uuid "F5053601-F75F-39AB-8FB7-AD3A4919953A")
    ("/0123456789:;<=>?@ABCDEFGH" #uuid "E3219C41-1396-32E5-9A03-8BED1F9F3BCC")
    ("0123456789:;<=>?@ABCDEFGHI" #uuid "94486C0C-5882-3490-9B5C-FDA8823BCD9C")
    ("123456789:;<=>?@ABCDEFGHIJ" #uuid "ABCB5CCC-7BCA-3388-B8A9-CA8888672F1B")
    ("23456789:;<=>?@ABCDEFGHIJK" #uuid "96C62C75-1B95-3A99-A68B-C0A98AF6652F")
    ("3456789:;<=>?@ABCDEFGHIJKL" #uuid "F93BE46D-0C6F-34E0-94EA-E7390A3BFAAD")
    ("456789:;<=>?@ABCDEFGHIJKLM" #uuid "8F558F64-472C-341E-B49E-A6743F54CC16")
    ("56789:;<=>?@ABCDEFGHIJKLMN" #uuid "2F4B6958-3FB4-3FD4-952A-861A7919A25C")
    ("6789:;<=>?@ABCDEFGHIJKLMNO" #uuid "FC83A94A-75AF-3E47-A633-10E784D5E311")
    ("789:;<=>?@ABCDEFGHIJKLMNOP" #uuid "9DA1321F-1A84-3FAB-A494-307C0E37938D")
    ("89:;<=>?@ABCDEFGHIJKLMNOPQ" #uuid "FC9CED61-35EF-3386-BF8E-31F9DA844323")
    ("9:;<=>?@ABCDEFGHIJKLMNOPQR" #uuid "80E91EB9-0298-30C8-B582-750262D42D4B")
    (":;<=>?@ABCDEFGHIJKLMNOPQRS" #uuid "2862A3CE-0115-39F9-A08F-4F26445C7C07")
    (";<=>?@ABCDEFGHIJKLMNOPQRST" #uuid "94CDB337-4C2D-3AAD-BDE4-A895060CC460")
    ("<=>?@ABCDEFGHIJKLMNOPQRSTU" #uuid "55F086BC-9EB0-3F2E-B0BC-F0BF637475D4")
    ("=>?@ABCDEFGHIJKLMNOPQRSTUV" #uuid "9F6FC7D5-7353-306D-BCDA-37B166A48755")
    (">?@ABCDEFGHIJKLMNOPQRSTUVW" #uuid "E5DA63AF-8285-300E-A518-2F553A2BBC7D")
    ("?@ABCDEFGHIJKLMNOPQRSTUVWX" #uuid "54962C09-F4CC-3EAC-B2A8-F4CF6E53C86D")
    ("@ABCDEFGHIJKLMNOPQRSTUVWXY" #uuid "D0CA9015-8176-3490-A119-1FBBA7833F7C")
    ("ABCDEFGHIJKLMNOPQRSTUVWXYZ" #uuid "1F1723EB-AEB7-32C6-9221-B43CF93434AE")
    ("BCDEFGHIJKLMNOPQRSTUVWXYZ[" #uuid "990A8C1A-01A9-3088-B275-A0EDCF171118")
    ("CDEFGHIJKLMNOPQRSTUVWXYZ[\\" #uuid "E54DC0F1-8DAC-3372-8938-4C77BBF4A795")
    ("DEFGHIJKLMNOPQRSTUVWXYZ[\\]" #uuid "DACA530A-D409-3DB5-AC3E-16FAC891EB8B")
    ("EFGHIJKLMNOPQRSTUVWXYZ[\\]^" #uuid "EC762B89-AE25-34DD-B267-77BA166D4C39")
    ("FGHIJKLMNOPQRSTUVWXYZ[\\]^_" #uuid "D99667DF-7B0A-335D-8889-31D54CC1D0C0")
    ("GHIJKLMNOPQRSTUVWXYZ[\\]^_`" #uuid "E1BD4B67-B324-3D93-B3AD-F2F7F5BC346C")
    ("HIJKLMNOPQRSTUVWXYZ[\\]^_`a" #uuid "9BA6966F-5425-3816-B53A-CAEDBB979028")
    ("IJKLMNOPQRSTUVWXYZ[\\]^_`ab" #uuid "E25019D1-332E-376B-BA06-5EA4EBF8FC8A")
    ("JKLMNOPQRSTUVWXYZ[\\]^_`abc" #uuid "B3BC1638-4A04-3B4C-9191-82D1D70356CF")
    ("KLMNOPQRSTUVWXYZ[\\]^_`abcd" #uuid "2F528BEC-A02E-3844-9267-B45FE9E7A860")
    ("LMNOPQRSTUVWXYZ[\\]^_`abcde" #uuid "6B13CD75-382B-334C-B511-797C9305DB8C")
    ("MNOPQRSTUVWXYZ[\\]^_`abcdef" #uuid "CFE5D94D-6634-3011-B616-9D634DCC7533")
    ("NOPQRSTUVWXYZ[\\]^_`abcdefg" #uuid "7D2E3761-8E9C-3828-96E6-854F4F678C81")
    ("OPQRSTUVWXYZ[\\]^_`abcdefgh" #uuid "FE71897C-B88F-36CA-9765-757A82E24AE4")
    ("PQRSTUVWXYZ[\\]^_`abcdefghi" #uuid "6101A664-0DB3-30DD-B04D-F8B6BA0EA255")
    ("QRSTUVWXYZ[\\]^_`abcdefghij" #uuid "EB769B0A-F62A-3EA3-BA7D-191471D4A016")
    ("RSTUVWXYZ[\\]^_`abcdefghijk" #uuid "628CB7F4-CC8E-34C1-A905-44D3558C247B")
    ("STUVWXYZ[\\]^_`abcdefghijkl" #uuid "1AE38A2F-EDF5-3BC4-AD63-E005EF35077E")
    ("TUVWXYZ[\\]^_`abcdefghijklm" #uuid "24C25E2E-CF08-3216-8BBC-521C4BD61AFC")
    ("UVWXYZ[\\]^_`abcdefghijklmn" #uuid "E023C41B-3A85-3AB3-A18C-A87FFC84CF53")
    ("VWXYZ[\\]^_`abcdefghijklmno" #uuid "27B13D02-E89E-3E30-8ABB-A43F8A8858FD")
    ("WXYZ[\\]^_`abcdefghijklmnop" #uuid "5DC9BA7C-30FC-3476-BBC3-FD45898C24AF")
    ("XYZ[\\]^_`abcdefghijklmnopq" #uuid "BF707BBE-53B0-3E29-BF53-BFE88A1BAE5D")
    ("YZ[\\]^_`abcdefghijklmnopqr" #uuid "B8837DD6-6BF0-3D9F-BDE4-D2D6AFF011F0")
    ("Z[\\]^_`abcdefghijklmnopqrs" #uuid "A88482D3-07EB-3FFF-B030-7D35B4E372F1")
    ("[\\]^_`abcdefghijklmnopqrst" #uuid "06FA4DAE-2EC9-3E9F-A01C-1DEC8F009F47")
    ("\\]^_`abcdefghijklmnopqrstu" #uuid "33B53814-8A07-3AAB-85BB-0897254CF7D3")
    ("]^_`abcdefghijklmnopqrstuv" #uuid "75543166-1164-3C52-9E18-688690CF9750")
    ("^_`abcdefghijklmnopqrstuvw" #uuid "F5D74A6C-A216-310B-9568-652A0640E277")
    ("_`abcdefghijklmnopqrstuvwx" #uuid "21D2CD4B-5CEC-3C0C-ABF4-87A256F37745")
    ("`abcdefghijklmnopqrstuvwxy" #uuid "C520DDB7-E9E5-3D05-9157-F2157DEB08CE")
    ("abcdefghijklmnopqrstuvwxyz" #uuid "6B95E2CC-5E76-365C-9C53-5DDAD84E612A")
    ("bcdefghijklmnopqrstuvwxyz{" #uuid "DEDE6550-14B9-3213-9660-661C46879AC1")
    ("cdefghijklmnopqrstuvwxyz{|" #uuid "4DC61324-36A9-3305-97FA-04F8461A8656")
    ("defghijklmnopqrstuvwxyz{|}" #uuid "ED828E85-BB8D-327B-92AD-366F29E9C804")
    ("efghijklmnopqrstuvwxyz{|}~" #uuid "925484B7-F24E-35FA-B607-6625301C99E9")
    ("fghijklmnopqrstuvwxyz{|}~ " #uuid "3EDFBE37-E082-3C35-97EB-9547B53E8E49")
    ("ghijklmnopqrstuvwxyz{|}~ !" #uuid "8A18A7D7-3C2B-3594-8C1C-9DE815BD26AD")
    ("hijklmnopqrstuvwxyz{|}~ !\"" #uuid "C1834810-8486-32CD-9EEC-90FFDCF84D28")
    ("ijklmnopqrstuvwxyz{|}~ !\"#" #uuid "42CFD52A-8177-3F5C-899A-586FB8B86BB4")
    ("jklmnopqrstuvwxyz{|}~ !\"#$" #uuid "7535FABD-FA47-3569-B74D-D1FE338503FB")
    ("klmnopqrstuvwxyz{|}~ !\"#$%" #uuid "2EEA92DB-5A9F-3469-9CA9-AC7391EBD520")
    ("lmnopqrstuvwxyz{|}~ !\"#$%&" #uuid "8A13DB2B-ACC9-3A40-AF19-B283691239D9")
    ("mnopqrstuvwxyz{|}~ !\"#$%&'" #uuid "AFF4E386-6231-3E2A-88F1-3FE63D467059")
    ("nopqrstuvwxyz{|}~ !\"#$%&'(" #uuid "E81B7969-464A-3B48-AC6A-C780732B94B3")
    ("opqrstuvwxyz{|}~ !\"#$%&'()" #uuid "316E4D11-F765-32E4-97B9-D3704000BD57")
    ("pqrstuvwxyz{|}~ !\"#$%&'()*" #uuid "3BF51E5F-6940-3591-96AC-0322900FB4DE")
    ("qrstuvwxyz{|}~ !\"#$%&'()*+" #uuid "8DB58107-A4C3-3560-A914-7547B57A0565")
    ("rstuvwxyz{|}~ !\"#$%&'()*+," #uuid "037874F2-EFCA-3DAB-9AD3-411B46D4C185")
    ("stuvwxyz{|}~ !\"#$%&'()*+,-" #uuid "B3F2E765-46DE-3082-ABFF-5B65801BF1E7")
    ("tuvwxyz{|}~ !\"#$%&'()*+,-." #uuid "4FD026E5-5478-3DD5-AF5A-EAE806EEF62D")
    ("uvwxyz{|}~ !\"#$%&'()*+,-./" #uuid "3142B451-58AC-3055-9D0D-9948A1F3706C")
    ("vwxyz{|}~ !\"#$%&'()*+,-./0" #uuid "741E5817-5E0F-31D8-8FAE-3AF54EB27365")
    ("wxyz{|}~ !\"#$%&'()*+,-./01" #uuid "A983541C-66E6-33E6-99C0-9D7F7750899B")
    ("xyz{|}~ !\"#$%&'()*+,-./012" #uuid "56F776F7-7F8C-3A05-84B8-E6FD416C4940")
    ("yz{|}~ !\"#$%&'()*+,-./0123" #uuid "4B0029B8-A2CE-30D8-9147-99DC8AC92BAC")
    ("z{|}~ !\"#$%&'()*+,-./01234" #uuid "91EB3CE3-9F35-36CB-AA35-F40E96960E4E")
    ("{|}~ !\"#$%&'()*+,-./012345" #uuid "B89FD471-22CA-3D4A-B255-14479084F964")
    ("|}~ !\"#$%&'()*+,-./0123456" #uuid "4E07B5E6-66EB-370B-A710-97064AE6D845")
    ("}~ !\"#$%&'()*+,-./01234567" #uuid "F23EFB3D-C9BF-3198-ACC6-38C646818FFF")
    ("~ !\"#$%&'()*+,-./012345678" #uuid "8099A277-2EE2-3A97-AAF1-7D186DEEBBDC")
    ))


(deftest check-v3-null-ns-cases
  (testing "v3 null-ns case-based correctness..."
    (doseq [case +v3-null-ns-cases+]
      (is (= (second case) (uuid/v3 uuid/+null+ (first case)))))))


(def +v3-dns-ns-cases+
  '((" !\"#$%&'()*+,-./0123456789" #uuid "8502CBB1-B406-39A3-AC93-00AE0DC70F89")
    ("!\"#$%&'()*+,-./0123456789:" #uuid "5CD1F6D6-BDE7-3951-B13A-A09645DC80B4")
    ("\"#$%&'()*+,-./0123456789:;" #uuid "3E5E99E8-0EFB-3F3F-B8A6-873B8EF8F95B")
    ("#$%&'()*+,-./0123456789:;<" #uuid "C75A5D34-955A-3D4F-B8F6-ED670CEFD20E")
    ("$%&'()*+,-./0123456789:;<=" #uuid "9062C80A-709E-3890-BA62-E01A7A9F5BE6")
    ("%&'()*+,-./0123456789:;<=>" #uuid "92168DE8-02D8-3E43-BF5F-BB96F9081EEF")
    ("&'()*+,-./0123456789:;<=>?" #uuid "5AE75650-AA07-3116-94B5-651E17E890B7")
    ("'()*+,-./0123456789:;<=>?@" #uuid "60F0E242-6A03-3710-BC32-E673CD132FB8")
    ("()*+,-./0123456789:;<=>?@A" #uuid "6B04570A-F602-3669-AC1D-1FE799A3B5F6")
    (")*+,-./0123456789:;<=>?@AB" #uuid "FA527A0F-C8F0-3608-8351-4D006D8E4F62")
    ("*+,-./0123456789:;<=>?@ABC" #uuid "72375C7F-1B61-3851-A4C6-D65839931A36")
    ("+,-./0123456789:;<=>?@ABCD" #uuid "9315034D-29BB-39C9-B29C-2AA2C4921EC8")
    (",-./0123456789:;<=>?@ABCDE" #uuid "C61EE942-70D4-3F61-B411-5D71667081C4")
    ("-./0123456789:;<=>?@ABCDEF" #uuid "929650E9-EAFB-3AC2-9945-95B254699094")
    ("./0123456789:;<=>?@ABCDEFG" #uuid "3751476F-2EAE-3CE1-954E-7CBFA8B46061")
    ("/0123456789:;<=>?@ABCDEFGH" #uuid "2910586F-51A0-353E-A5C3-C40F0C14F410")
    ("0123456789:;<=>?@ABCDEFGHI" #uuid "B91DF409-A8CB-351A-AC0B-24E4A6A7FD28")
    ("123456789:;<=>?@ABCDEFGHIJ" #uuid "2A9784FF-AA2E-3974-86BF-B37F0E60F50B")
    ("23456789:;<=>?@ABCDEFGHIJK" #uuid "A91A7B75-6674-30AB-94FF-ED6F93728BEA")
    ("3456789:;<=>?@ABCDEFGHIJKL" #uuid "932F664F-3289-3527-9D52-0EA2C0A205AB")
    ("456789:;<=>?@ABCDEFGHIJKLM" #uuid "FF6A35C9-0BD7-314C-BF4C-DABF76F8BBF9")
    ("56789:;<=>?@ABCDEFGHIJKLMN" #uuid "AA5AD585-6B5D-3303-BBCD-EB493EC61DE6")
    ("6789:;<=>?@ABCDEFGHIJKLMNO" #uuid "E88EC15E-AC84-3295-868F-9CAB7FC1D15B")
    ("789:;<=>?@ABCDEFGHIJKLMNOP" #uuid "36AD17D3-9EB5-3222-B944-DF80AD71CE8C")
    ("89:;<=>?@ABCDEFGHIJKLMNOPQ" #uuid "75477B3F-47CC-3EC8-ABD2-5D8C2B700C00")
    ("9:;<=>?@ABCDEFGHIJKLMNOPQR" #uuid "B76D9FDC-C337-34C2-9EA2-95BF83BB4FF3")
    (":;<=>?@ABCDEFGHIJKLMNOPQRS" #uuid "481DD83F-9686-39F9-AA46-FE893E69EF78")
    (";<=>?@ABCDEFGHIJKLMNOPQRST" #uuid "763F4C35-D29D-3223-98EC-1ED581A3D4B9")
    ("<=>?@ABCDEFGHIJKLMNOPQRSTU" #uuid "DC2C9ECD-B365-3D76-A989-2964B7AB10E0")
    ("=>?@ABCDEFGHIJKLMNOPQRSTUV" #uuid "20BC5EB3-B6BD-322D-97C2-FCE1F65271BB")
    (">?@ABCDEFGHIJKLMNOPQRSTUVW" #uuid "8A6E735D-D1EB-33E1-88BB-F0E30A369318")
    ("?@ABCDEFGHIJKLMNOPQRSTUVWX" #uuid "0EB2F01D-560A-3616-BCC3-2C0E10612EA5")
    ("@ABCDEFGHIJKLMNOPQRSTUVWXY" #uuid "C3B2FCB2-1CE3-33AF-98DC-824DAA7A5327")
    ("ABCDEFGHIJKLMNOPQRSTUVWXYZ" #uuid "DBAE940B-391B-3655-B21F-2AE528FAD04C")
    ("BCDEFGHIJKLMNOPQRSTUVWXYZ[" #uuid "08B4ED9D-791A-369F-BBF7-F4FCEBE803F5")
    ("CDEFGHIJKLMNOPQRSTUVWXYZ[\\" #uuid "907267C2-5EB6-36DC-9104-E9B036075927")
    ("DEFGHIJKLMNOPQRSTUVWXYZ[\\]" #uuid "F9F89EBA-AA31-3353-8016-BE7E24D5FEA5")
    ("EFGHIJKLMNOPQRSTUVWXYZ[\\]^" #uuid "A8A80596-3613-39DF-82D1-D5B8C1789ED3")
    ("FGHIJKLMNOPQRSTUVWXYZ[\\]^_" #uuid "0DAFC143-7019-30AA-999B-21C8EFDE44C8")
    ("GHIJKLMNOPQRSTUVWXYZ[\\]^_`" #uuid "BDE827C3-2DB7-39D2-8361-17C5177C3A9B")
    ("HIJKLMNOPQRSTUVWXYZ[\\]^_`a" #uuid "D47F238E-AEC0-3D16-A68F-557FE161D6F4")
    ("IJKLMNOPQRSTUVWXYZ[\\]^_`ab" #uuid "737E81D8-B110-3333-8BAA-9F215028E353")
    ("JKLMNOPQRSTUVWXYZ[\\]^_`abc" #uuid "26C803EF-6B0D-387E-BB11-DFC748546F92")
    ("KLMNOPQRSTUVWXYZ[\\]^_`abcd" #uuid "BBAA20C9-5428-31AC-99C7-4C5959E77F5F")
    ("LMNOPQRSTUVWXYZ[\\]^_`abcde" #uuid "C7655287-1D3A-392A-B29D-DECB40F34D60")
    ("MNOPQRSTUVWXYZ[\\]^_`abcdef" #uuid "0295F24F-EE5E-3D05-BCA3-56BD9960CFD2")
    ("NOPQRSTUVWXYZ[\\]^_`abcdefg" #uuid "911D25F2-D390-31CA-ACFF-6E6FBA5B7DDD")
    ("OPQRSTUVWXYZ[\\]^_`abcdefgh" #uuid "528A84FD-9E97-303A-92E7-51645F93525F")
    ("PQRSTUVWXYZ[\\]^_`abcdefghi" #uuid "AB4FB6D1-4F74-3331-98EF-C27E87008D07")
    ("QRSTUVWXYZ[\\]^_`abcdefghij" #uuid "23DA9F4B-5218-3C3E-AE38-1A4F760E5C93")
    ("RSTUVWXYZ[\\]^_`abcdefghijk" #uuid "98A14203-0B09-3095-83BC-0FFB9A4684C4")
    ("STUVWXYZ[\\]^_`abcdefghijkl" #uuid "CA10A029-E06A-364E-A0ED-B7AA4F24D207")
    ("TUVWXYZ[\\]^_`abcdefghijklm" #uuid "2C95761B-FBD7-33E2-936A-13D8D0C48F58")
    ("UVWXYZ[\\]^_`abcdefghijklmn" #uuid "E3C6A320-23A7-353E-8E06-C363EC783A86")
    ("VWXYZ[\\]^_`abcdefghijklmno" #uuid "32EFCA32-AD04-3CB2-BA66-E8509FC1C743")
    ("WXYZ[\\]^_`abcdefghijklmnop" #uuid "9203D568-7AE8-3F1E-BD83-AA72C1F47495")
    ("XYZ[\\]^_`abcdefghijklmnopq" #uuid "6860513A-1DCF-3B63-AC85-E684406A2F61")
    ("YZ[\\]^_`abcdefghijklmnopqr" #uuid "564B821A-DEAC-3A72-89BC-5659B8883FB8")
    ("Z[\\]^_`abcdefghijklmnopqrs" #uuid "6988DFAF-7A52-3F4B-AE03-BFE348977A37")
    ("[\\]^_`abcdefghijklmnopqrst" #uuid "CF62DC30-B276-39F5-9FFD-AA571936C5FD")
    ("\\]^_`abcdefghijklmnopqrstu" #uuid "8715D8EC-89BB-395F-A393-95EFF3396E25")
    ("]^_`abcdefghijklmnopqrstuv" #uuid "C66A5CB0-3695-31F8-8C41-D061DB8B76FD")
    ("^_`abcdefghijklmnopqrstuvw" #uuid "0045436E-446F-3A24-86A2-5D3547DFFA18")
    ("_`abcdefghijklmnopqrstuvwx" #uuid "5E24D6DF-AF96-3F19-8A71-89EEF9F19083")
    ("`abcdefghijklmnopqrstuvwxy" #uuid "4D0B435F-C0F9-3329-BDCB-F3FC3AABEDD4")
    ("abcdefghijklmnopqrstuvwxyz" #uuid "E7684C6A-B70E-3531-9426-3BC6E033B0FE")
    ("bcdefghijklmnopqrstuvwxyz{" #uuid "486F569D-7C28-33C2-8B16-4227BD39E3D3")
    ("cdefghijklmnopqrstuvwxyz{|" #uuid "DAE05C81-A7FD-3BE3-824C-789FD9F57A64")
    ("defghijklmnopqrstuvwxyz{|}" #uuid "15F225A9-BF75-352A-BEBA-03BDEDCA0C56")
    ("efghijklmnopqrstuvwxyz{|}~" #uuid "F27B144E-D8A9-3D01-923D-E81296D994D4")
    ("fghijklmnopqrstuvwxyz{|}~ " #uuid "040C3AF4-3765-3383-BD52-9258CE747EE4")
    ("ghijklmnopqrstuvwxyz{|}~ !" #uuid "1751D35C-113B-399C-B8EC-B40D0ADD30BE")
    ("hijklmnopqrstuvwxyz{|}~ !\"" #uuid "CD19EAB7-86F6-3DEB-8BDB-A382B8939580")
    ("ijklmnopqrstuvwxyz{|}~ !\"#" #uuid "C6AAFEEA-0794-3BA2-BBB0-C019DEBEA642")
    ("jklmnopqrstuvwxyz{|}~ !\"#$" #uuid "E566FDE3-FB60-347F-8A61-CE4E8314C7D8")
    ("klmnopqrstuvwxyz{|}~ !\"#$%" #uuid "3E44EBAC-F311-3801-B576-3D570247E085")
    ("lmnopqrstuvwxyz{|}~ !\"#$%&" #uuid "7C3E1262-380A-3970-ACC2-8C67C95AB416")
    ("mnopqrstuvwxyz{|}~ !\"#$%&'" #uuid "8C802F36-4223-3A16-A002-7CF4333CA2BC")
    ("nopqrstuvwxyz{|}~ !\"#$%&'(" #uuid "68E9670E-DDB7-3235-9E22-BDE736C58160")
    ("opqrstuvwxyz{|}~ !\"#$%&'()" #uuid "619A2D6F-8DE7-389E-92C9-DCE56B0CF5F4")
    ("pqrstuvwxyz{|}~ !\"#$%&'()*" #uuid "F5D251CF-76C2-3C0F-BBEB-13AE71B90EDD")
    ("qrstuvwxyz{|}~ !\"#$%&'()*+" #uuid "71AE2AA0-47C1-307A-9BE7-73B6BFEE0AE6")
    ("rstuvwxyz{|}~ !\"#$%&'()*+," #uuid "0BB601E4-94D6-3AD0-8BFC-28CDA8AAD2CD")
    ("stuvwxyz{|}~ !\"#$%&'()*+,-" #uuid "CABDBD7F-54AE-37F5-B7C8-DFE9219133CE")
    ("tuvwxyz{|}~ !\"#$%&'()*+,-." #uuid "FEE30B3E-FAF2-3465-AC90-17D6BEC041FF")
    ("uvwxyz{|}~ !\"#$%&'()*+,-./" #uuid "D37DD9FB-6DD9-362B-9BAA-E3A67E7C3A15")
    ("vwxyz{|}~ !\"#$%&'()*+,-./0" #uuid "365BED8E-5E7C-3704-AD7B-4A5B91767D2E")
    ("wxyz{|}~ !\"#$%&'()*+,-./01" #uuid "0C99C105-BAC3-3D4B-B4F5-04251CF48B7B")
    ("xyz{|}~ !\"#$%&'()*+,-./012" #uuid "E06D9F72-813B-3F3E-A096-E4C7FC05BD84")
    ("yz{|}~ !\"#$%&'()*+,-./0123" #uuid "16DF5C3B-9E1D-3CE9-A8F4-165B7067756C")
    ("z{|}~ !\"#$%&'()*+,-./01234" #uuid "D3196C68-D1A0-3CE5-9DA7-4F360C0E528B")
    ("{|}~ !\"#$%&'()*+,-./012345" #uuid "F9EA7DC9-F790-3EBC-B970-FE944FC25078")
    ("|}~ !\"#$%&'()*+,-./0123456" #uuid "786245E9-B956-3774-BD77-3DD581271B30")
    ("}~ !\"#$%&'()*+,-./01234567" #uuid "3BAC4AC0-3FD1-371F-AC81-5BA1AB950E18")
    ("~ !\"#$%&'()*+,-./012345678" #uuid "6EC555CD-9DFF-3783-B556-2D8DD55A5CD0")
    ))


(deftest check-v3-dns-ns-cases
  (testing "v3 dns-ns case-based correctness..."
    (doseq [case +v3-dns-ns-cases+]
      (is (= (second case) (uuid/v3 uuid/+namespace-dns+ (first case)))))))


(def +v3-oid-ns-cases+
  '((" !\"#$%&'()*+,-./0123456789" #uuid "43D9B705-E75C-31EB-A5AE-89D03DEA6C8A")
    ("!\"#$%&'()*+,-./0123456789:" #uuid "D425D486-816F-310F-9675-D9F0F5568AAD")
    ("\"#$%&'()*+,-./0123456789:;" #uuid "411A971C-E960-3B93-B02D-40E492EAD382")
    ("#$%&'()*+,-./0123456789:;<" #uuid "4DF246BF-DBE6-337E-BC7F-A2C18B8F9264")
    ("$%&'()*+,-./0123456789:;<=" #uuid "D72384E9-DF60-3B90-A18B-CA71ADBBEFA1")
    ("%&'()*+,-./0123456789:;<=>" #uuid "6CB6E4D1-F590-329D-9CB3-11B985B81333")
    ("&'()*+,-./0123456789:;<=>?" #uuid "C2C9252E-6018-3704-A0EB-786BEE2C949B")
    ("'()*+,-./0123456789:;<=>?@" #uuid "F162EFDE-3FDC-31A0-8BBD-281E3911DB83")
    ("()*+,-./0123456789:;<=>?@A" #uuid "4A4327AA-B253-3A36-9D51-FD149EF61FFC")
    (")*+,-./0123456789:;<=>?@AB" #uuid "E6D5600D-6965-3AFD-946F-BC3E1E3E51D3")
    ("*+,-./0123456789:;<=>?@ABC" #uuid "36A30022-28CD-3AFE-87F1-B84974ECF06D")
    ("+,-./0123456789:;<=>?@ABCD" #uuid "B9E15BA0-1574-3AA2-8B85-AD7D264F861F")
    (",-./0123456789:;<=>?@ABCDE" #uuid "4047ABD8-DAB4-318A-90F7-423759DC796F")
    ("-./0123456789:;<=>?@ABCDEF" #uuid "86E64E13-7028-39BD-80FB-912D77416CC3")
    ("./0123456789:;<=>?@ABCDEFG" #uuid "75FDCE21-970F-3046-8D8A-693FB255746B")
    ("/0123456789:;<=>?@ABCDEFGH" #uuid "52A2192D-BD14-334D-B9B4-2756FADC303B")
    ("0123456789:;<=>?@ABCDEFGHI" #uuid "3AC1FF81-E7F2-3F2B-A4BB-0CF77AC9878B")
    ("123456789:;<=>?@ABCDEFGHIJ" #uuid "0AE96CC9-7371-3ACF-8D78-ACF5FD1E42F6")
    ("23456789:;<=>?@ABCDEFGHIJK" #uuid "223AC51D-323A-3889-B3AB-5733CC79479A")
    ("3456789:;<=>?@ABCDEFGHIJKL" #uuid "A9288FC0-599A-3C05-9EFC-96DB4D59451B")
    ("456789:;<=>?@ABCDEFGHIJKLM" #uuid "EE795374-5D2F-38C4-B0DC-BD2E72E03FEB")
    ("56789:;<=>?@ABCDEFGHIJKLMN" #uuid "80E415B6-32DF-350B-8AF2-388EEB0AE788")
    ("6789:;<=>?@ABCDEFGHIJKLMNO" #uuid "BD6755C9-5565-3ADD-8B37-F53284B1DBD5")
    ("789:;<=>?@ABCDEFGHIJKLMNOP" #uuid "30CFFF63-A342-30E4-9692-846588A5702B")
    ("89:;<=>?@ABCDEFGHIJKLMNOPQ" #uuid "B419E181-2D73-3619-B809-FCC0B518EE68")
    ("9:;<=>?@ABCDEFGHIJKLMNOPQR" #uuid "C795BEAB-5B07-3D8C-ACC2-374CBE19669F")
    (":;<=>?@ABCDEFGHIJKLMNOPQRS" #uuid "21FAADF5-3A76-34B4-B835-A80D9E269A91")
    (";<=>?@ABCDEFGHIJKLMNOPQRST" #uuid "EF0A7F62-8C69-30B2-B66B-E480C7BC71FE")
    ("<=>?@ABCDEFGHIJKLMNOPQRSTU" #uuid "159FC9E2-5A10-3C8E-8DC1-D8D9C0CB7706")
    ("=>?@ABCDEFGHIJKLMNOPQRSTUV" #uuid "F6882739-0246-3AF0-AB7C-267644CEA4DD")
    (">?@ABCDEFGHIJKLMNOPQRSTUVW" #uuid "C004A348-6266-3D52-B3BB-ADE1F48A9D49")
    ("?@ABCDEFGHIJKLMNOPQRSTUVWX" #uuid "681545F9-D596-34EF-9F10-C6D8F87087F1")
    ("@ABCDEFGHIJKLMNOPQRSTUVWXY" #uuid "9C78D8DD-918F-331A-A9C4-69CE5C2505F0")
    ("ABCDEFGHIJKLMNOPQRSTUVWXYZ" #uuid "1AE24819-DE5A-327D-89FE-2745E611B9F3")
    ("BCDEFGHIJKLMNOPQRSTUVWXYZ[" #uuid "E6ACEB11-DEEB-36D7-A281-1E3C9F653C2B")
    ("CDEFGHIJKLMNOPQRSTUVWXYZ[\\" #uuid "71300878-7997-3AB1-BE58-4A1879169575")
    ("DEFGHIJKLMNOPQRSTUVWXYZ[\\]" #uuid "F3E984D7-CACF-302F-8E30-616634DEC171")
    ("EFGHIJKLMNOPQRSTUVWXYZ[\\]^" #uuid "9DABDA54-3905-3443-BFE4-3680CB5D4CB6")
    ("FGHIJKLMNOPQRSTUVWXYZ[\\]^_" #uuid "BB9F5584-4772-3D83-9AA2-7808B78E346E")
    ("GHIJKLMNOPQRSTUVWXYZ[\\]^_`" #uuid "B9160282-CEBB-3AD3-A52F-4DC60393798D")
    ("HIJKLMNOPQRSTUVWXYZ[\\]^_`a" #uuid "89BB630C-4C3C-317F-BA91-D3E12DF00FF7")
    ("IJKLMNOPQRSTUVWXYZ[\\]^_`ab" #uuid "4A9C2A72-F9D6-3650-8721-FC8E0EF635FB")
    ("JKLMNOPQRSTUVWXYZ[\\]^_`abc" #uuid "F3DABB7C-2C7F-3AE5-9214-D85BF57910CB")
    ("KLMNOPQRSTUVWXYZ[\\]^_`abcd" #uuid "671A4498-CE67-314C-B50F-06212DCF5AC4")
    ("LMNOPQRSTUVWXYZ[\\]^_`abcde" #uuid "3FC4800A-A5C3-3454-A407-2D874993D0A8")
    ("MNOPQRSTUVWXYZ[\\]^_`abcdef" #uuid "862A6E34-653C-3D90-9BC4-DF5F875F4F49")
    ("NOPQRSTUVWXYZ[\\]^_`abcdefg" #uuid "13423AE6-E6C4-3F4A-A94D-E9A5B5DD7707")
    ("OPQRSTUVWXYZ[\\]^_`abcdefgh" #uuid "24F418E9-C5BA-3A82-8492-334ADFD63CD1")
    ("PQRSTUVWXYZ[\\]^_`abcdefghi" #uuid "A1B3F1E1-E25F-3AE9-88C8-1BF76FB82CA7")
    ("QRSTUVWXYZ[\\]^_`abcdefghij" #uuid "D84E1825-DD1A-3147-8275-1A6CF04E60EC")
    ("RSTUVWXYZ[\\]^_`abcdefghijk" #uuid "9B293E2C-6A27-3A79-82FF-C225846296CD")
    ("STUVWXYZ[\\]^_`abcdefghijkl" #uuid "F17A6EC8-F42E-3523-ADDC-51AB5F08EF2B")
    ("TUVWXYZ[\\]^_`abcdefghijklm" #uuid "7C17D37D-49D1-3815-8F3D-4F2950B3D863")
    ("UVWXYZ[\\]^_`abcdefghijklmn" #uuid "71E14392-3F2A-3EC9-9476-027B4D7F9EDA")
    ("VWXYZ[\\]^_`abcdefghijklmno" #uuid "7103789B-4358-3BF3-B7E9-0B3C203E7552")
    ("WXYZ[\\]^_`abcdefghijklmnop" #uuid "7C95A201-3F6F-36CF-9DBB-E1BCCD9D9FD2")
    ("XYZ[\\]^_`abcdefghijklmnopq" #uuid "A05E1B2E-63BC-38D9-9333-56E6337C14ED")
    ("YZ[\\]^_`abcdefghijklmnopqr" #uuid "34515391-27CF-3202-9AF8-624095EC7D7F")
    ("Z[\\]^_`abcdefghijklmnopqrs" #uuid "FA32549C-6874-3DAC-B69B-AE22C3EF2411")
    ("[\\]^_`abcdefghijklmnopqrst" #uuid "E6C8FC6A-240B-382C-9FDD-7856BCFF4EA1")
    ("\\]^_`abcdefghijklmnopqrstu" #uuid "2C6677D4-BDD9-3A9C-9755-5F6F06D53A54")
    ("]^_`abcdefghijklmnopqrstuv" #uuid "EA5349C6-5861-3C7F-9A6F-ED4F5FE37AE9")
    ("^_`abcdefghijklmnopqrstuvw" #uuid "F93457EF-D9AD-3151-91A4-57BE63AEAC4A")
    ("_`abcdefghijklmnopqrstuvwx" #uuid "A29821DC-23A0-3CB9-88FC-2F5C4B4EDE24")
    ("`abcdefghijklmnopqrstuvwxy" #uuid "AB8D5F5D-D180-34B3-B320-DE0FA700664F")
    ("abcdefghijklmnopqrstuvwxyz" #uuid "0C491E79-D2B7-3A30-B9C4-BE9515E6E0E2")
    ("bcdefghijklmnopqrstuvwxyz{" #uuid "E2D1EFD6-229C-3868-BBF1-5143CF5E3649")
    ("cdefghijklmnopqrstuvwxyz{|" #uuid "F8BAE8BC-E1A4-3D42-8015-AF897E3AA1E4")
    ("defghijklmnopqrstuvwxyz{|}" #uuid "AF0061CD-2B25-3ECC-BFD4-8707FC33E3F4")
    ("efghijklmnopqrstuvwxyz{|}~" #uuid "AE4EBA8B-95F9-32CF-A37F-0A202D4D746E")
    ("fghijklmnopqrstuvwxyz{|}~ " #uuid "AF3A5813-EB8C-3DA2-9BDA-866C2D255DB6")
    ("ghijklmnopqrstuvwxyz{|}~ !" #uuid "D547E557-7542-3241-A56D-D5B9A44D4BFF")
    ("hijklmnopqrstuvwxyz{|}~ !\"" #uuid "FA78A373-D9B9-3EBF-8699-642816516351")
    ("ijklmnopqrstuvwxyz{|}~ !\"#" #uuid "57D1E4B6-BE59-3692-BD09-07C62A4570B6")
    ("jklmnopqrstuvwxyz{|}~ !\"#$" #uuid "B7946A08-1F5A-3EA4-8040-422179089B5E")
    ("klmnopqrstuvwxyz{|}~ !\"#$%" #uuid "BB50DA00-95FE-37D8-A9B4-847555CFA047")
    ("lmnopqrstuvwxyz{|}~ !\"#$%&" #uuid "54DEC298-B336-3690-BA29-182C6B358BE7")
    ("mnopqrstuvwxyz{|}~ !\"#$%&'" #uuid "1181FADE-BC28-3BA1-9594-61848F97CD74")
    ("nopqrstuvwxyz{|}~ !\"#$%&'(" #uuid "8BB47B9F-237D-313D-9B71-A897646ACF43")
    ("opqrstuvwxyz{|}~ !\"#$%&'()" #uuid "2E548DFD-1A9D-3EBF-AB39-41DC1530F39D")
    ("pqrstuvwxyz{|}~ !\"#$%&'()*" #uuid "4082F972-90D3-3011-A16E-D3ADAB93A1C9")
    ("qrstuvwxyz{|}~ !\"#$%&'()*+" #uuid "181BACA8-4476-377E-AC33-739E1B49CDFA")
    ("rstuvwxyz{|}~ !\"#$%&'()*+," #uuid "FB6E8638-AA96-3C2A-A621-2319E9604BA4")
    ("stuvwxyz{|}~ !\"#$%&'()*+,-" #uuid "C30943D9-FD02-3922-AD5E-3911F0B460F8")
    ("tuvwxyz{|}~ !\"#$%&'()*+,-." #uuid "80F5F58A-4C81-3E25-BC67-681F523F5A47")
    ("uvwxyz{|}~ !\"#$%&'()*+,-./" #uuid "1656D4B3-F6C4-3774-AF81-7BEEE3C64A8A")
    ("vwxyz{|}~ !\"#$%&'()*+,-./0" #uuid "05DA3CB1-6634-3BEE-A275-E5D672F2B303")
    ("wxyz{|}~ !\"#$%&'()*+,-./01" #uuid "6CA9378F-0A4F-3F79-9D07-76D37D080F4C")
    ("xyz{|}~ !\"#$%&'()*+,-./012" #uuid "795D525B-B8AB-36EC-9069-C0B108434526")
    ("yz{|}~ !\"#$%&'()*+,-./0123" #uuid "55DD78E1-5ECB-3E9E-9D52-90C0D0D4F9B0")
    ("z{|}~ !\"#$%&'()*+,-./01234" #uuid "DF4A937F-5E7C-372B-8CAD-96778829C4D0")
    ("{|}~ !\"#$%&'()*+,-./012345" #uuid "5AC9C9D3-446A-3D44-81C0-B3BA2D5F72D9")
    ("|}~ !\"#$%&'()*+,-./0123456" #uuid "D5A24A2C-A19F-314F-A2D1-4DEAFB6DF09A")
    ("}~ !\"#$%&'()*+,-./01234567" #uuid "0BE200A8-CCF3-39E6-B784-28ABCF933676")
    ("~ !\"#$%&'()*+,-./012345678" #uuid "B1BD83C9-B045-3416-A411-2091009AA351")
    ))


(deftest check-v3-oid-ns-cases
  (testing "v3 oid-ns case-based correctness..."
    (doseq [case +v3-oid-ns-cases+]
      (is (= (second case) (uuid/v3 uuid/+namespace-oid+ (first case)))))))
