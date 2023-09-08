package com.example.contacts

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.contacts.Util.callPhoneNumber
import com.example.contacts.Util.messagePhoneNumber
import com.example.contacts.databinding.ActivityDetailBinding
import com.google.android.material.snackbar.Snackbar

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private var selectedImageUri: Uri? = null
    private val EDIT_IMAGE_REQUEST_CODE_DETAIL = 6
    private lateinit var editImage: ImageView

    companion object {
        private lateinit var detailContact: Contact
        private var contactPosition = 0


        val CONTACT_POSITION = "contact_position"
        val CONTACT_ITEM = "contact_item"

        fun newIntentForDetail(context: Context?, contact: Contact, position: Int) =
            Intent(context, DetailActivity::class.java).apply {
                detailContact = contact
                contactPosition = position
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.apply {
            tvMobile.text = detailContact.phoneNumber
            tvEmail.text = detailContact.email
            tvName.text = detailContact.name
            if (detailContact.isNew) {
                Glide.with(binding.root.context)
                    .load(detailContact.profileImageUri)
                    .into(binding.ivUser)
            } else {
                binding.ivUser.setImageResource(detailContact.photo)
            }

            if (detailContact.bookmark) {
                binding.bookmark.setBackgroundResource(R.drawable.clicked_bookmark)
            } else {
                binding.bookmark.setBackgroundResource(R.drawable.unclicked_bookmark)
            }

            binding.bookmark.setOnClickListener {

                detailContact.bookmark = !detailContact.bookmark


                if (detailContact.bookmark) {
                    binding.bookmark.setBackgroundResource(R.drawable.clicked_bookmark)
                    showSnackBarMessage("                                      ⭐즐찾⭐")
                } else {
                    binding.bookmark.setBackgroundResource(R.drawable.unclicked_bookmark)
                    showSnackBarMessage("                                   ⭐즐찾해제⭐")
                }
//                val resultIntent = Intent()
//                resultIntent.putExtra("BOOKMARK", detailContact.bookmark)
//                resultIntent.putExtra("PHONE", detailContact.phoneNumber)
//
//                setResult(RESULT_OK, resultIntent)

            }
            // 뒤로가기 실행 시 데이터 넘겨줌
            btnBackPressed.setOnClickListener {
                detailContact.name = tvName.text.toString()
                detailContact.phoneNumber = tvMobile.text.toString()
                detailContact.email = tvEmail.text.toString()
                detailContact.profileImageUri = selectedImageUri


                val resultIntent = Intent()
//                resultIntent.putExtra("BOOKMARK", detailContact.bookmark)
//                resultIntent.putExtra("PHONE", detailContact.phoneNumber)
                resultIntent.putExtra(CONTACT_ITEM, detailContact) // 객체 콜백으로 전송
                resultIntent.putExtra(CONTACT_POSITION, contactPosition) // 수정에 필요한 position
                setResult(RESULT_OK, resultIntent)

                finish()
            }

            fabCall.setOnClickListener {
                callPhoneNumber(this@DetailActivity, detailContact.phoneNumber)
            }
            fabMessage.setOnClickListener {
                messagePhoneNumber(this@DetailActivity, detailContact.phoneNumber)
            }
        }


        binding.editBtn.setOnClickListener {

            val builder = AlertDialog.Builder(this)
            builder.setTitle("EDIT")

            val v1 = layoutInflater.inflate(R.layout.edit_dialog, null)
            builder.setView(v1)

            val editName: EditText = v1.findViewById(R.id.et_edit_name)
            val editPhoneNumber: EditText = v1.findViewById(R.id.et_edit_phone_number)
            val editEmail: EditText = v1.findViewById(R.id.et_edit_email)
            val ibEditImg: ImageButton = v1.findViewById(R.id.ib_edit_img)
            editImage = v1.findViewById(R.id.iv_edit_img)
            editImage.setImageDrawable(binding.ivUser.drawable)

            // 갤러리에서 이미지 선택을 위한 코드
            ibEditImg.setOnClickListener {
                val intent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, EDIT_IMAGE_REQUEST_CODE_DETAIL)
            }

            // p0에 해당 AlertDialog가 들어온다. findViewById를 통해 view를 가져와서 사용
            val listener = DialogInterface.OnClickListener { p0, p1 ->
                val alert = p0 as AlertDialog

                // 값 변경
                binding.tvName.text = editName.text.toString()
                binding.tvMobile.text = editPhoneNumber.text.toString()
                binding.tvEmail.text = editEmail.text.toString()

                // 이미지 설정
                selectedImageUri?.let { uri ->
                    editImage.setImageURI(uri)
                    binding.ivUser.setImageURI(uri) // ivUser에도 이미지 설정
                }

                // 다이얼로그 닫기
                alert.dismiss()
            }

            // 확인 버튼을 다이얼로그에 대한 클릭 리스너로 설정
            builder.setPositiveButton("확인", listener)

            val alertDialog = builder.create() // AlertDialog를 생성합니다.

            alertDialog.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == EDIT_IMAGE_REQUEST_CODE_DETAIL && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            detailContact.isNew = true
            try {
                editImage.setImageURI(selectedImageUri)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "이미지 설정에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showSnackBarMessage(message: String) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        snackbar.show()
    }

}