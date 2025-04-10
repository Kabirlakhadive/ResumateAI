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
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

class Template1 {

    suspend fun generateResume(context: Context, jsonObject: JSONObject) {


        try {
            var adjustValue = 0
            val fileName = "resume.pdf"
            val file = File(context.filesDir, fileName)
            var documentSize = A4

            do {
                val scaleFactor = max(0.5f, (1 - adjustValue * 0.1f)) // Ensures minimum 50% scaling
                val textScaleFactor = 1 - (1 - scaleFactor) / 4

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
                val document = Document(pdfDoc, documentSize)

                /* <------------------------- Load All the Values -----------------------------------------> */

                val sectionOrder = jsonObject.getJSONArray("sectionsOrder")
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

                val allCaps = headingJson.getBoolean("allCaps")
                val backgroundFullLine = headingJson.getBoolean("backgroundFullLine")

                val nameJson = jsonObject.getJSONObject("name")
                val nameSize = (nameJson.getDouble("size")).toFloat() * textScaleFactor
                val nameColor = getColorFromHex(nameJson.getString("color"))
                val nameAlignment = when (nameJson.getString("alignment").lowercase()) {
                    "center" -> TextAlignment.CENTER
                    "left" -> TextAlignment.LEFT
                    "right" -> TextAlignment.RIGHT
                    else -> TextAlignment.CENTER
                }
                val nameMarginBottom = (nameJson.getDouble("marginBottom")).toFloat() * scaleFactor
                val nameMarginTop = (nameJson.getDouble("marginTop")).toFloat() * scaleFactor

                val marginJson = jsonObject.getJSONObject("margin")

                document.setMargins(
                    marginJson.getInt("topMargin").toFloat() * scaleFactor,
                    marginJson.getInt("rightMargin").toFloat() * scaleFactor,
                    marginJson.getInt("bottomMargin").toFloat() * scaleFactor,
                    marginJson.getInt("leftMargin").toFloat() * scaleFactor
                )

                /*<-----------------------------Helper functions for adding elements--------------------------------->*/


                // Add Normal Text
                fun addText(text: String) {
                    document.add(
                        Paragraph(text)
                            .setFont(normalFont)
                            .setFontSize(textSize)
                            .setMultipliedLeading(textSpacing)
                            .setMarginBottom(textMargin)
                            .setMarginTop(textMargin)
                            .setFontColor(textColor)
                    )
                }

                // Add Line Separator
                fun addLine() {
                    val solidLine = SolidLine().apply {
                        lineWidth = lineSize
                        color = lineColor
                    }
                    document.add(LineSeparator(solidLine))
                }

                // Add Heading with Styling
                fun addHeading(text: String) {
                    val headingJson = jsonObject.getJSONObject("headings")

                    val headingText =
                        Text(if (headingJson.getBoolean("allCaps")) text.uppercase() else text)
                            .setFont(headerFont)
                            .setFontSize(headingSize)
                            .setFontColor(headingColor)
                            .setBackgroundColor(headingBackground)

                    val paragraph = Paragraph().add(headingText)
                        .setPaddingLeft(headingPadding)
                        .setMarginTop(textMargin)  // Ensure spacing above the heading
                        .setMarginBottom(textMargin) // Ensure spacing below the heading

                    // Apply background color if needed
                    if (headingJson.getBoolean("backgroundFullLine")) {
                        paragraph.setBackgroundColor(headingBackground)
                            .setPadding(headingPadding)  // Apply uniform padding to prevent background issues
                    }

                    document.add(paragraph)

                    if (jsonObject.getJSONObject("line").getBoolean("line")) {
                        addLine() // Ensure line is added correctly
                    }
                }


                // Add a Table with Two Cells
                fun addTable(cell1: String, cell2: String?) {
                    val table = Table(floatArrayOf(1f, 1f))
                        .setWidth(UnitValue.createPercentValue(100f))
                        .setMargin(0f)  // Ensure no external margins
                        .setPadding(0f) // Remove internal padding

                    val cellLeft = Cell().add(
                        Paragraph(cell1)
                            .setFontSize(textSize)
                            .setFont(boldFont)
                            .setMargin(0f) // Remove any margin inside the paragraph
                            .setPadding(0f) // Remove padding from the paragraph
                    )
                        .setBorder(NO_BORDER)
                        .setFontColor(textColor)
                        .setPadding(0f) // Ensure no padding in cell
                        .setMargin(0f)  // Ensure no margin in cell

                    val cellRight = Cell().add(
                        Paragraph(cell2 ?: "")
                            .setFontSize(textSize)
                            .setFont(boldFont)
                            .setTextAlignment(TextAlignment.RIGHT)
                            .setMargin(0f)
                            .setPadding(0f)
                    )
                        .setBorder(NO_BORDER)
                        .setFontColor(textColor)
                        .setPadding(0f)
                        .setMargin(0f)

                    table.addCell(cellLeft)
                    table.addCell(cellRight)

                    document.add(table)
                }


                // Add Name Section at the Top
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

                    document.add(nameParagraph)

                }

                // Add Contact Information Below Name
                fun addContact() {
                    val contactJson = jsonObject.getJSONObject("contact")
                    val separator = contactJson.getString("separator")
                    val contactText = "${userProfile?.phoneNumber} $separator ${userProfile?.email}"

                    val contactParagraph = Paragraph(contactText)
                        .setFont(normalFont)
                        .setFontSize(textSize - 2f)
                        .setTextAlignment(nameAlignment)
                        .setFontColor(getColorFromHex(contactJson.getString("color")))
                        .setPadding(contactJson.getDouble("paddingVertical").toFloat())
                        .setMarginBottom(nameMarginBottom)

                    if (contactJson.getBoolean("backgroundColor")) {
                        contactParagraph.setBackgroundColor(getColorFromHex(contactJson.getString("background")))
                    }

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

                // Set Background Color for the Page
                fun addBackgroundColor(color: DeviceRgb) {
                    val page = pdfDoc.getPage(1)
                    val pageSize = page.pageSize
                    val canvas = PdfCanvas(page)

                    canvas.saveState()
                        .setFillColor(color)
                        .rectangle(0.0, 0.0, pageSize.width.toDouble(), pageSize.height.toDouble())
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

                fun createHeading(title: String): Paragraph {
                    val text = if (allCaps) title.uppercase() else title

                    val paragraph = Paragraph(text)
                        .setFont(headerFont)
                        .setFontSize(headingSize)
                        .setFontColor(headingColor)
                        .setBackgroundColor(headingBackground)
                        .setMarginBottom(textMargin)
                    if (backgroundFullLine) {
                        paragraph.setBackgroundColor(headingBackground)
                            .setPaddingLeft(5f)
                    }
                    return paragraph

                }


                fun formatDate(dateStr: String): String {
                    return try {
                        val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val outputFormat = SimpleDateFormat(
                            jsonObject.getString("dateFormat"),
                            Locale.getDefault()
                        )
                        val date = inputFormat.parse(dateStr)
                        outputFormat.format(
                            date ?: return dateStr
                        ) // If parsing fails, return the original string
                    } catch (e: Exception) {
                        Log.d("PDFGenerator", "Error formatting date: ${e.message}")
                        Log.d("PDFGenerator", "Input date string: $dateStr")
                        Log.d("PDFGenerator", "Input date format: dd/MM/yyyy")
                        Log.d("PDFGenerator", "Output date format: ${jsonObject.getString("date")}")
                        dateStr // Fallback if date conversion fails
                    }
                }

                /*
                    <---------------------- add content of the document below -------------------------------->
                 */


                val page = pdfDoc.addNewPage()
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

                for (i in 0 until sectionOrder.length()) {
                    when (sectionOrder.getString(i)) {
                        "name" -> addTop()

                        "contact" -> addContact()

                        "objective" -> {
                            if (jsonObject.getJSONObject("objective").getBoolean("heading")) {
                                addHeading("Objective")
                                addText(userProfile?.objective ?: "Objective Unavailable")
                            } else {
                                addText(userProfile?.objective ?: "Objective Unavailable")
                            }
                        }

                        "education" -> {
                            addHeading("Education")

                            education.forEach {
                                val score = when (it.gradingSystem) {
                                    "Percentage" -> "${it.score}%"
                                    "CGPA" -> "${it.score} CGPA"
                                    else -> "${it.score} Percentile"
                                }

                                val details = buildString {
                                    append(it.degree)
                                    if (!it.isBranchEmpty()) append(" in ${it.branch}")
                                    if (!it.isSpecializationEmpty()) append(" with ${it.specialization}")
                                    append(" ($score)")
                                }

                                addTable(it.school, it.year)
                                addText(details)

                                if (it.board.isNotEmpty()) addText(it.board)
                            }
                        }

                        "skills" -> {
                            addHeading("Technical Skills")
                            addText(allSkills)
                        }

                        "experience" -> {
                            addHeading("Experience")
                            experiences.forEach {
                                addTable(
                                    "${it.position} at ${it.companyName}",
                                    "${formatDate(it.startDate)} - ${formatDate(it.endDate.toString())}"
                                )
                                addText(it.output.toString())
                                Log.d(
                                    "PDFGenerator",
                                    "Formated Date : ${formatDate(it.startDate)} - ${formatDate(it.endDate.toString())}"
                                )
                                Log.d(
                                    "PDFGenerator",
                                    "Date : ${it.startDate} - ${it.endDate.toString()}"
                                )
                            }
                        }

                        "projects" -> {
                            addHeading("Projects")
                            projects.forEach {
                                addTable(it.title, " ")
                                addText(it.output.toString())
                            }
                        }

                        "certifications" -> {
                            if (certifications.isNotEmpty()) {
                                addHeading("Certifications")
                                certifications.forEach {
                                    addTable(it.name, it.year)
                                }
                            }
                        }

                        "achievements" -> {
                            if (achievements.isNotEmpty()) {
                                addHeading("Achievements")
                                achievements.forEach {
                                    addTable(it.title, it.date)
                                }
                            }
                        }

                        "experience-projects" -> {
                            val experienceDiv = Div()
                            val projectDiv = Div()

                            // Add headings
                            experienceDiv.add(createHeading("Experience"))
                            projectDiv.add(createHeading("Projects"))

                            // Add experiences dynamically
                            experiences.forEach {
                                experienceDiv.add(
                                    Paragraph("${it.position} at ${it.companyName}")
                                        .setFontSize(textSize)
                                        .setFont(boldFont)
                                        .setFontColor(textColor)
                                        .setMarginBottom(textMargin)
                                )

                                experienceDiv.add(
                                    Paragraph("${it.startDate} - ${it.endDate}")
                                        .setFont(normalFont)
                                        .setFontSize(textSize)
                                        .setFontColor(textColor)
                                        .setMarginBottom(textMargin)
                                )

                                experienceDiv.add(
                                    Paragraph(it.output)
                                        .setFont(normalFont)
                                        .setFontSize(textSize)
                                        .setFontColor(textColor)
                                        .setMarginBottom(textMargin)
                                )
                            }

                            // Add projects dynamically
                            projects.forEach {
                                projectDiv.add(
                                    Paragraph(it.title)
                                        .setFontSize(textSize)
                                        .setFont(boldFont)
                                        .setFontColor(textColor)
                                        .setMarginBottom(textMargin)
                                )

                                projectDiv.add(
                                    Paragraph(it.output)
                                        .setFont(normalFont)
                                        .setFontSize(textSize)
                                        .setFontColor(textColor)
                                        .setMarginBottom(textMargin)
                                )
                            }

                            // Create a table to hold both sections side by side
                            val table = Table(floatArrayOf(1f, 1f))
                            table
                                .setWidth(UnitValue.createPercentValue(100f))
                                .setMarginBottom(textMargin)

                            // Add experience and project sections to the table
                            if (jsonObject.getJSONObject("page").getBoolean("verticalLine")) {
                                table.addCell(
                                    Cell().add(experienceDiv).setBorder(NO_BORDER)
                                        .setBorderRight(SolidBorder(ColorConstants.BLACK, 1f))
                                )
                            } else {
                                table.addCell(Cell().add(experienceDiv).setBorder(NO_BORDER))
                            }
                            table.addCell(Cell().add(projectDiv).setBorder(NO_BORDER))

                            // Add the table to the document
                            document.add(table)
                        }

                        "education - experience" -> {
                            val educationDiv = Div()
                            val experienceDiv = Div()
                            educationDiv.add(createHeading("Education"))
                            experienceDiv.add(createHeading("Experience"))
                            education.forEach {
                                educationDiv.add(
                                    Paragraph("${it.school}, ${it.year}")
                                        .setFont(normalFont)
                                        .setFontSize(textSize)
                                        .setFont(boldFont)
                                        .setFontColor(textColor)
                                        .setMarginBottom(textMargin)
                                )

                                educationDiv.add(
                                    Paragraph(
                                        it.degree + if (!it.isBranchEmpty()) " in ${it.branch}" else "" +
                                                if (!it.isSpecializationEmpty()) " with ${it.specialization}" else "" +
                                                        " (${
                                                            when (it.gradingSystem) {
                                                                "Percentage" -> "${it.score}%"
                                                                "CGPA" -> "${it.score} CGPA"
                                                                else -> "${it.score} Percentile"
                                                            }
                                                        })"
                                    )
                                        .setFont(normalFont)
                                        .setFontSize(textSize)
                                        .setFontColor(textColor)
                                        .setMarginBottom(textMargin)
                                )

                                if (it.board.isNotEmpty()) {
                                    educationDiv.add(
                                        Paragraph(it.board)
                                            .setFont(normalFont)
                                            .setFontSize(textSize)
                                            .setFontColor(textColor)
                                            .setMarginBottom(textMargin)
                                    )
                                }
                            }
                            experiences.forEach {
                                experienceDiv.add(
                                    Paragraph("${it.position} at ${it.companyName}")
                                        .setFontSize(textSize)
                                        .setFont(boldFont)
                                        .setFontColor(textColor)
                                        .setMarginBottom(textMargin)
                                )

                                experienceDiv.add(
                                    Paragraph("${it.startDate} - ${it.endDate}")
                                        .setFont(normalFont)
                                        .setFontSize(textSize)
                                        .setFontColor(textColor)
                                        .setMarginBottom(textMargin)
                                )

                                experienceDiv.add(
                                    Paragraph(it.output)
                                        .setFont(normalFont)
                                        .setFontSize(textSize)
                                        .setFontColor(textColor)
                                        .setMarginBottom(textMargin)
                                )
                            }

                            val table = Table(floatArrayOf(1f, 1f))
                            table
                                .setWidth(UnitValue.createPercentValue(100f))
                                .setMarginBottom(textMargin)

                            // Add experience and project sections to the table
                            if (jsonObject.getJSONObject("page").getBoolean("verticalLine")) {
                                table.addCell(
                                    Cell().add(educationDiv).setBorder(NO_BORDER)
                                        .setBorderRight(SolidBorder(ColorConstants.BLACK, 1f))
                                )
                            } else {
                                table.addCell(Cell().add(educationDiv).setBorder(NO_BORDER))
                            }
                            table.addCell(Cell().add(experienceDiv).setBorder(NO_BORDER))

                            // Add the table to the document
                            document.add(table)


                        }
                    }
                }

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


