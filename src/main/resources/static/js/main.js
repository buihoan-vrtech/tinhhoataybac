document.addEventListener("DOMContentLoaded", function () {

    const slides = document.querySelectorAll(".slide");
    const dots = document.querySelectorAll(".dot");

    const prevButton = document.querySelector(".prev");
    const nextButton = document.querySelector(".next");

    let currentSlide = 0;

    let autoSlide;


    function showSlide(index) {

        slides.forEach(function (slide) {
            slide.classList.remove("active");
        });

        dots.forEach(function (dot) {
            dot.classList.remove("active");
        });


        if (index >= slides.length) {
            currentSlide = 0;
        }

        if (index < 0) {
            currentSlide = slides.length - 1;
        }


        slides[currentSlide].classList.add("active");

        dots[currentSlide].classList.add("active");
    }


    function nextSlide() {

        currentSlide++;

        showSlide(currentSlide);

    }


    function prevSlide() {

        currentSlide--;

        showSlide(currentSlide);

    }


    function startAutoSlide() {

        autoSlide = setInterval(function () {

            nextSlide();

        }, 3000);

    }


    function resetAutoSlide() {

        clearInterval(autoSlide);

        startAutoSlide();

    }


    nextButton.addEventListener("click", function () {

        nextSlide();

        resetAutoSlide();

    });


    prevButton.addEventListener("click", function () {

        prevSlide();

        resetAutoSlide();

    });


    dots.forEach(function (dot, index) {

        dot.addEventListener("click", function () {

            currentSlide = index;

            showSlide(currentSlide);

            resetAutoSlide();

        });

    });


    showSlide(currentSlide);

    startAutoSlide();
    document.addEventListener("DOMContentLoaded", function () {

        const quantityInput = document.querySelector("#quantity");
        const minusButton = document.querySelector(".quantity-btn.minus");
        const plusButton = document.querySelector(".quantity-btn.plus");

        if (quantityInput && minusButton && plusButton) {

            minusButton.addEventListener("click", function () {

                let value = parseInt(quantityInput.value) || 1;

                if (value > 1) {
                    quantityInput.value = value - 1;
                }
            });


            plusButton.addEventListener("click", function () {

                let value = parseInt(quantityInput.value) || 1;

                const max = parseInt(quantityInput.max);

                if (!max || value < max) {
                    quantityInput.value = value + 1;
                }
            });
        }

    });
});