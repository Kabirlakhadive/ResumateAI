package com.example.buildairesume.basetemplates

import android.content.Context
import android.util.Log
import com.example.buildairesume.UserData
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.events.IEventHandler
import com.itextpdf.kernel.events.PdfDocumentEvent
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize.A4
import com.itextpdf.kernel.geom.PageSize.TABLOID
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.action.PdfAction
import com.itextpdf.kernel.pdf.annot.PdfLinkAnnotation
import com.itextpdf.kernel.pdf.canvas.PdfCanvas
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border.*
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max


class Template2 {

    suspend fun generateResume(context: Context, jsonObject: JSONObject) {

        try {
            var adjustValue = 0
            val fileName = "resume.pdf"
            val file = File(context.filesDir, fileName)
            var documentSize = A4

            do {
                val scaleFactor = max(0.5f, (1 - adjustValue * 0.1f)) // Ensures minimum 50% scaling
                val textScaleFactor = 1 - (1 - scaleFactor) / 4

                Log.d("PDFGenerator", "ScaleFactor : $scaleFactor")

                // Convert hex color to DeviceRgb
                fun getColorFromHex(hex: String): DeviceRgb {
                    return DeviceRgb(
                        Integer.valueOf(hex.substring(1, 3), 16),
                        Integer.valueOf(hex.substring(3, 5), 16),
                        Integer.valueOf(hex.substring(5, 7), 16)
                    )
                }


                val writer = PdfWriter(FileOutputStream(file))
                val pdfDoc = PdfDocument(writer)
                val document = Document(pdfDoc, A4)

                /* <------------------------- Load All the Values -----------------------------------------> */

                val db = UserData.getInstance(context)
                val userProfile = withContext(Dispatchers.IO) { db.userProfileDao().getUser() }
                val projects = withContext(Dispatchers.IO) { db.projectDao().getAllProjects() }
                val experiences =
                    withContext(Dispatchers.IO) { db.experienceDao().getAllExperiences() }
                val skills = withContext(Dispatchers.IO) { db.skillsDao().getAllSkills() }
                val education = withContext(Dispatchers.IO) { db.educationDao().getAllEducation() }
                val certifications =
                    withContext(Dispatchers.IO) { db.certificationDao().getAllCertifications() }
                val achievements =
                    withContext(Dispatchers.IO) { db.achievementDao().getAllAchievements() }
                val allSkills = skills.joinToString(", ") { it.skillName }


                val boldFont = loadFont(context, jsonObject.getJSONObject("name").getString("font"))
                val normalFont =
                    loadFont(context, jsonObject.getJSONObject("text").getString("font"))
                val headerFont =
                    loadFont(context, jsonObject.getJSONObject("headings").getString("font"))

                val textJson = jsonObject.getJSONObject("text")
                val textMargin = (textJson.getDouble("verticalMargin")).toFloat() * scaleFactor
                val textSize = (textJson.getDouble("size")).toFloat() * textScaleFactor
                val textSpacing = (textJson.getDouble("spacing")).toFloat()
                val textColor = getColorFromHex(textJson.getString("color"))

                val lineJson = jsonObject.getJSONObject("line")
                val lineColor = getColorFromHex(lineJson.getString("color"))
                val lineSize = lineJson.getDouble("width").toFloat()

                val headingJson = jsonObject.getJSONObject("headings")
                val headingSize = (headingJson.getDouble("size")).toFloat() * textScaleFactor
                val headingColor = getColorFromHex(headingJson.getString("color"))
                val headingBackground = getColorFromHex(headingJson.getString("background"))
                val headingPadding = (headingJson.getDouble("paddingHorizontal")).toFloat()


                val nameJson = jsonObject.getJSONObject("name")
                val nameSize = (nameJson.getDouble("size")).toFloat() * textScaleFactor
                val nameColor = getColorFromHex(nameJson.getString("color"))
                val nameSpacing = (nameJson.getDouble("marginBottom")).toFloat() * scaleFactor
                val nameAlignment = when (nameJson.getString("alignment").lowercase()) {
                    "center" -> TextAlignment.CENTER
                    "left" -> TextAlignment.LEFT
                    "right" -> TextAlignment.RIGHT
                    else -> TextAlignment.CENTER
                }
                val nameMarginBottom = (nameJson.getDouble("marginBottom")).toFloat() * scaleFactor
                val nameMarginTop = (nameJson.getDouble("marginTop")).toFloat() * scaleFactor
                val nameBackground = (getColorFromHex(nameJson.getString("background")))


                document.setMargins(
                    30f * scaleFactor,
                    30f * scaleFactor,
                    30f * scaleFactor,
                    30f * scaleFactor
                )


                /*<-----------------------------Helper functions for adding elements--------------------------------->*/


                // Add Normal Text
                fun addText(text: String) {
                    document.add(
                        Paragraph(text)
                            .setFont(normalFont)
                            .setFontSize(
                                jsonObject.getJSONObject("text").getDouble("size").toFloat()
                            )
                            .setMultipliedLeading(
                                jsonObject.getJSONObject("text").getDouble("spacing").toFloat()
                            )
                            .setMarginBottom(
                                jsonObject.getJSONObject("text").getDouble("verticalMargin")
                                    .toFloat()
                            )
                            .setMarginTop(
                                jsonObject.getJSONObject("text").getDouble("verticalMargin")
                                    .toFloat()
                            )
                            .setFontColor(
                                getColorFromHex(
                                    jsonObject.getJSONObject("text").getString("color")
                                )
                            )
                    )

                }

                // if line are in the document edit below
                fun addLine() {
                    val solidLine = SolidLine().apply {
                        lineWidth = lineSize
                        color = lineColor
                    }

                    // Add the colored line separator
                    document.add(
                        LineSeparator(solidLine)
                    )
                }


                //add the top name and below that address contact and email in small letters
                fun addTop() {


                    val nameText = Text(userProfile?.fullName ?: "Name Unavailable")
                        .setFont(boldFont)
                        .setFontSize(nameSize)
                        .setFontColor(nameColor)


                    val nameParagraph = Paragraph()
                        .add(nameText)
                        .setTextAlignment(nameAlignment)
                        .setMarginBottom(nameMarginBottom)
                        .setMarginTop(nameMarginTop)
                        .setMultipliedLeading(nameSpacing)

                    document.add(nameParagraph)

                }

                fun addContact() {
                    val contactJson = jsonObject.getJSONObject("contact")
                    val separator = contactJson.getString("separator")
                    val contactText = "${userProfile?.phoneNumber} $separator ${userProfile?.email}"

                    val contactParagraph = Paragraph(contactText)
                        .setFont(normalFont)
                        .setFontSize(textSize - 2f)
                        .setTextAlignment(nameAlignment)
                        .setBackgroundColor(getColorFromHex(contactJson.getString("background")))
                        .setPadding(contactJson.getDouble("paddingVertical").toFloat())
                        .setMarginBottom(nameMarginBottom)

                    document.add(contactParagraph)

                    if (jsonObject.getJSONObject("line").getBoolean("line")) {
                        document.add(
                            LineSeparator(SolidLine())
                                .setMarginBottom(
                                    jsonObject.getJSONObject("name").getDouble("marginBottom")
                                        .toFloat()
                                )
                                .setStrokeColor(
                                    getColorFromHex(
                                        jsonObject.getJSONObject("line").getString("color")
                                    )
                                )
                        )
                    }
                }

                fun addBackgroundColor(color: DeviceRgb) {
                    val page = pdfDoc.getPage(1)
                    val pageSize = page.pageSize
                    val canvas = PdfCanvas(page)

                    canvas.saveState()
                        .setFillColor(color)
                        .rectangle(
                            0.0, 0.0, pageSize.width.toDouble(), pageSize.height.toDouble()
                        )
                        .fill()
                        .restoreState()
                }

                fun addImageBackground(context: Context, pdfDoc: PdfDocument) {
                    val page = pdfDoc.getPage(1)
                    val pageSize = page.pageSize
                    val canvas = PdfCanvas(page)

                    try {
                        // Open the image from assets
                        val imagePath =
                            jsonObject.getJSONObject("page").getString("backgroundImage")
                        val imageStream = context.assets.open(imagePath)
                        val imageData = ImageDataFactory.create(imageStream.readBytes())
                        imageStream.close()

                        // Scale and position the image to cover the full page
                        canvas.addImageWithTransformationMatrix(
                            imageData,
                            pageSize.width, 0f, 0f, pageSize.height, 0f, 0f
                        )

                    } catch (e: Exception) {
                        Log.e("PDFGenerator", "Error loading background image: ${e.message}")
                    }
                }

                // Function to create a clickable annotation
                fun addLinkAnnotation(
                    page: PdfPage,
                    x: Float,
                    y: Float,
                    text: String,
                    url: String?
                ) {
                    val width = text.length * 5.5f  // Approximate text width
                    val annotation = PdfLinkAnnotation(Rectangle(x, y, width, 10f))
                        .setAction(PdfAction.createURI(url))
                    page.addAnnotation(annotation)
                }

                fun addFooterWithLinks(pdfDoc: PdfDocument) {
                    pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, IEventHandler { event ->
                        val docEvent = event as PdfDocumentEvent
                        val page = docEvent.page
                        val pdfCanvas = PdfCanvas(page)
                        val pageSize = page.pageSize
                        val footerY = pageSize.bottom + 20  // Adjust footer position
                        val font = PdfFontFactory.createFont()

                        val githubText = "GitHub"
                        val websiteText = "Website"
                        val emailText = "Email"

                        val githubUrl = userProfile?.github
                        val websiteUrl = userProfile?.website
                        val emailUrl = userProfile?.email

                        // Define text positions
                        val xStart = pageSize.width / 2 - 100  // Centering
                        val spacing = 70f

                        // Draw Text
                        pdfCanvas.beginText()
                            .setFontAndSize(font, 10f)
                            .setColor(ColorConstants.BLUE, true)
                            .moveText(xStart.toDouble(), footerY.toDouble())
                            .showText(githubText)
                            .moveText(spacing.toDouble(), 0.toDouble())
                            .showText(websiteText)
                            .moveText(spacing.toDouble(), 0f.toDouble())
                            .showText(emailText)
                            .endText()

                        // Add Links

                        addLinkAnnotation(page, xStart, footerY - 2, githubText, githubUrl)
                        addLinkAnnotation(
                            page,
                            xStart + spacing,
                            footerY - 2,
                            websiteText,
                            websiteUrl
                        )
                        addLinkAnnotation(
                            page,
                            xStart + spacing * 2,
                            footerY - 2,
                            emailText,
                            emailUrl
                        )

                        pdfCanvas.release()
                    })
                }


                /*
                    <---------------------- add content of the document below -------------------------------->
                 */
                val page = pdfDoc.addNewPage()
                val pageSize = page.pageSize
                val canvas = PdfCanvas(page)

                if (jsonObject.getJSONObject("page").getBoolean("footer")) {
                    addFooterWithLinks(pdfDoc)
                }


                if (jsonObject.getJSONObject("page").getBoolean("imageBackground")) {
                    addImageBackground(context, pdfDoc)
                } else {
                    val backgroundColor =
                        getColorFromHex(jsonObject.getJSONObject("page").getString("background"))
                    addBackgroundColor(backgroundColor)
                }
                val leftSectionTopMargin =
                    jsonObject.getJSONObject("page").getInt("sectionLeftTopMargin").toFloat()
                // Create two Divs to hold the left and right sections
                val leftColumn = Div().setMarginTop(leftSectionTopMargin)
                val rightColumn = Div()
                val allCaps = headingJson.getBoolean("allCaps")
                val backgroundFullLine = headingJson.getBoolean("backgroundFullLine")

                if (jsonObject.getBoolean("nameAtTop")) {
                    addTop()
                }
                if (jsonObject.getBoolean("contactAtTop")) {
                    addContact()
                }
                // Function to create headings dynamically
                fun createHeading(title: String): Paragraph {
                    val text = if (allCaps) title.uppercase() else title
                    val isHeadingBackground =
                        jsonObject.getJSONObject("headings").getBoolean("backgroundColor")
                    val paragraph = if (isHeadingBackground) {
                        Paragraph(text)
                            .setFont(headerFont)
                            .setFontSize(headingSize)
                            .setFontColor(headingColor)
                            .setBackgroundColor(headingBackground)
                            .setMarginBottom(textMargin)
                    } else {
                        Paragraph(text)
                            .setFont(headerFont)
                            .setFontSize(headingSize)
                            .setFontColor(headingColor)
                            .setMarginBottom(textMargin)
                    }
                    if (backgroundFullLine && isHeadingBackground) {
                        paragraph.setBackgroundColor(headingBackground).setPaddingLeft(5f)
                    }
                    return paragraph
                }

                // Function to add content dynamically based on section type
                fun addSectionContent(section: String, div: Div) {
                    when (section) {
                        "name" -> {
                            val nameJson = jsonObject.getJSONObject("name")
                            val nameText = Text(userProfile?.fullName ?: "Name Unavailable")
                                .setFont(boldFont)
                                .setFontSize(nameSize)
                                .setFontColor(nameColor)

                            if (nameJson.getBoolean("backgroundColor")) {
                                nameText.setBackgroundColor(nameBackground)
                            }

                            div.add(
                                Paragraph().add(nameText).setTextAlignment(TextAlignment.CENTER)
                            )
                        }

                        "contact" -> {
                            if (!jsonObject.getBoolean("contactAtTop")) {
                                div.add(createHeading("Contact"))
                                div.add(
                                    Paragraph("${userProfile?.phoneNumber}")
                                        .setFont(normalFont)
                                        .setFontSize(textSize - 2f)
                                        .setTextAlignment(TextAlignment.LEFT)
                                        .setMarginBottom(textMargin)
                                )
                                div.add(
                                    Paragraph("${userProfile?.email}")
                                        .setFont(normalFont)
                                        .setFontSize(12f)
                                        .setTextAlignment(TextAlignment.LEFT)
                                        .setMarginBottom(textMargin)
                                )
                                div.add(
                                    Paragraph("${userProfile?.website}")
                                        .setFont(normalFont)
                                        .setFontSize(12f)
                                        .setTextAlignment(TextAlignment.LEFT)
                                        .setMarginBottom(textMargin)
                                )
                                div.add(
                                    Paragraph("${userProfile?.address}")
                                        .setFont(normalFont)
                                        .setFontSize(12f)
                                        .setTextAlignment(TextAlignment.LEFT)
                                        .setMarginBottom(textMargin)
                                )
                            }
                        }

                        "summary" -> {
                            div.add(createHeading("Summary"))
                            div.add(
                                Paragraph(userProfile?.objective ?: "No Summary Provided")
                                    .setFontSize(textSize)
                                    .setFontColor(textColor)
                            )
                        }

                        "skills" -> {
                            div.add(createHeading("Skills"))
                            val skillsText = allSkills
                            div.add(
                                Paragraph(skillsText)
                                    .setFontSize(textSize)
                                    .setFontColor(textColor)
                            )
                        }

                        "experience" -> {
                            div.add(createHeading("Experience"))
                            experiences.forEach {
                                div.add(
                                    Paragraph("${it.position} at ${it.companyName}")
                                        .setFontSize(textSize)
                                        .setFont(boldFont)
                                        .setFontColor(textColor)
                                        .setMarginBottom(textMargin)
                                        .setMarginTop(textMargin)
                                )
                                div.add(
                                    Paragraph("${it.startDate} - ${it.endDate}")
                                        .setFontSize(textSize)
                                        .setFontColor(textColor)
                                        .setMarginBottom(textMargin)
                                        .setMarginTop(textMargin)
                                )
                                div.add(
                                    Paragraph(it.output)
                                        .setFontSize(textSize)
                                        .setFontColor(textColor)
                                        .setMarginBottom(textMargin)
                                        .setMarginTop(textMargin)
                                )
                            }
                        }

                        "education" -> {
                            div.add(createHeading("Education"))

                            education.forEach {
                                // Construct the score format
                                val score = when (it.gradingSystem) {
                                    "Percentage" -> "${it.score}%"
                                    "CGPA" -> "${it.score} CGPA"
                                    else -> "${it.score} Percentile"
                                }

                                // Add school name and year
                                div.add(
                                    Paragraph("${it.school} (${it.year})")
                                        .setFontSize(textSize)
                                        .setFont(boldFont)
                                        .setFontColor(textColor)
                                        .setMarginBottom(textMargin)
                                        .setMarginTop(textMargin)
                                )

                                // Add qualification
                                div.add(
                                    Paragraph(it.qualification)
                                        .setFontSize(textSize)
                                        .setFontColor(textColor)
                                        .setMarginTop(textMargin)
                                        .setMarginBottom(textMargin)
                                )

                                // Add board if available
                                if (it.board.isNotEmpty()) {
                                    div.add(
                                        Paragraph(it.board)
                                            .setFontSize(textSize)
                                            .setFontColor(textColor)
                                            .setMarginTop(textMargin)
                                            .setMarginBottom(textMargin)
                                    )
                                }

                                // Add branch and specialization if available
                                if (!it.isBranchEmpty() || !it.isSpecializationEmpty()) {
                                    val specializationText =
                                        if (!it.isSpecializationEmpty()) " with ${it.specialization}" else ""
                                    div.add(
                                        Paragraph("${it.branch}$specializationText")
                                            .setFontSize(textSize)
                                            .setFontColor(textColor)
                                            .setMarginTop(textMargin)
                                            .setMarginBottom(textMargin)
                                    )
                                }

                                // Add degree and score
                                if (!it.isDegreeEmpty()) {
                                    div.add(
                                        Paragraph("${it.degree} ◦ $score")
                                            .setFontSize(textSize)
                                            .setFont(boldFont)
                                            .setFontColor(textColor)
                                            .setMarginBottom(textMargin)
                                    )
                                }

                                // Add spacing between entries
                                div.add(Paragraph("\n"))
                            }
                        }

                        "certifications" -> {
                            div.add(createHeading("Certifications"))
                            certifications.forEach {
                                div.add(
                                    Paragraph("${it.name}            (${it.year})")
                                        .setFontSize(textSize)
                                        .setFont(boldFont)
                                        .setFontColor(textColor)
                                        .setTextAlignment(TextAlignment.LEFT)
                                )
//                            div.add(
//                                Paragraph(it.year)
//                                    .setFontSize(textSize)
//                                    .setFontColor(textColor)
//                                    .setTextAlignment(TextAlignment.RIGHT)
//                            )
                            }
                        }

                        "projects" -> {
                            div.add(createHeading("Projects"))
                            projects.forEach {
                                div.add(
                                    Paragraph(it.title)
                                        .setFontSize(textSize)
                                        .setFont(boldFont)
                                        .setFontColor(textColor)
                                        .setMarginTop(textMargin)
                                        .setMarginBottom(textMargin)
                                )
                                div.add(
                                    Paragraph(it.output)
                                        .setFontSize(textSize)
                                        .setFontColor(textColor)
                                        .setMarginTop(textMargin)
                                        .setMarginBottom(textMargin)
                                )
                            }
                        }

                        "achievements" -> {
                            if (achievements.isNotEmpty()) {
                                div.add(createHeading("Achievements"))
                                achievements.forEach {
                                    div.add(
                                        Paragraph(it.title)
                                            .setFontSize(textSize)
                                            .setTextAlignment(TextAlignment.LEFT)
                                            .setMarginTop(textMargin)
                                            .setMarginBottom(textMargin)
                                    )
                                    div.add(
                                        Paragraph(it.date)
                                            .setFontSize(textSize)
                                            .setTextAlignment(TextAlignment.RIGHT)
                                            .setMarginTop(textMargin)
                                            .setMarginBottom(textMargin)
                                    )

                                }
                            }

                        }
                    }
                }

// Dynamically populate left and right sections based on the order from JSON
                val sectionLeftOrder = jsonObject.getJSONArray("sectionLeftOrder")
                for (i in 0 until sectionLeftOrder.length()) {
                    addSectionContent(sectionLeftOrder.getString(i), rightColumn)
                }

                val sectionRightOrder = jsonObject.getJSONArray("sectionRightOrder")
                for (i in 0 until sectionRightOrder.length()) {
                    addSectionContent(sectionRightOrder.getString(i), leftColumn)
                }


                val leftRatio = jsonObject.getString("leftRatio").toFloat()
                val rightRatio = jsonObject.getString("rightRatio").toFloat()
// Create a table with two columns for left and right sections
                val table = Table(floatArrayOf(leftRatio, rightRatio), true)
                table.setWidth(UnitValue.createPercentValue(100f))

                if (jsonObject.getJSONObject("page").getBoolean("verticalLine")) {
                    table.addCell(
                        Cell().add(leftColumn).setBorder(NO_BORDER)
                            .setBorderRight(SolidBorder(ColorConstants.BLACK, 1f))
                    )
                } else {
                    table.addCell(Cell().add(leftColumn).setBorder(NO_BORDER))
                }

                table.addCell(Cell().add(rightColumn).setBorder(NO_BORDER))

// Add the table to the document
                document.add(table)


                val pageCount = pdfDoc.numberOfPages
                /*
                    <---------------------- End the document and save in local folder -------------------------------->
                 */

                document.close()
                pdfDoc.close()

                Log.d("PDFGenerator", "PDF created with $pageCount pages")
                if (pageCount > 1 && scaleFactor > 0.5f) {
                    adjustValue += 1
                } else if (pageCount > 1 && documentSize == A4) {
                    documentSize = TABLOID // Change to Tabloid size if A4 is still too small
                    adjustValue = -3  // Reset scaling adjustments to try again with the new size

                    Log.d("PDFGenerator", "Changing to Tabloid size")
                } else {
                    break  // Stop shrinking when conditions are met
                }
            } while (pageCount > 1)
            Log.d("PDFGenerator", "Closing PDF document")
            Log.d("PDFGenerator", "PDF Saved in Downloads Folder(End of Generate Resume Function)")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("PDFGenerator", "Error creating PDF: ${e.message}")
        }
    }


    /*<-------------------------------Funciton to handle fonts ------------------------------>*/


    private fun loadFont(context: Context, fontFileName: String): PdfFont {
        return try {
            val assetManager = context.assets
            val inputStream: InputStream = assetManager.open("fonts/$fontFileName")
            val tempFile = File(context.filesDir, fontFileName)
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
//            PdfFontFactory.createFont(tempFile.absolutePath,"WinAnsi", "UTF-8")
            PdfFontFactory.createFont(tempFile.absolutePath, "WinAnsi")
        } catch (e: Exception) {
            Log.e("PDF", "Font Load Error: ${e.message}")
            PdfFontFactory.createFont()
        }
    }
}
