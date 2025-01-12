document.addEventListener('DOMContentLoaded', function () {
    // ボタンをクラスセレクタで取得
    const favoriteButtons = document.querySelectorAll('.favoriteButton');

    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    favoriteButtons.forEach(button => {
        button.addEventListener('click', async function () {
            const storeId = this.dataset.itemId;
            const isFavorited = this.dataset.favorited === 'true';
            const url = isFavorited
                ? `/api/favorites/remove/${storeId}`
                : `/api/favorites/add/${storeId}`;
            const method = isFavorited ? 'DELETE' : 'POST';

            try {
                const response = await fetch(url, {
                    method: method,
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json',
                        [csrfHeader]: csrfToken
                    }
                });

                if (!response.ok) {
                    throw new Error(`サーバーエラー: ${response.status}`);
                }

                const responseData = await response.json();

                // アラート表示
                alert(responseData.message || '操作が成功しました。');

                // クラスとデータ属性の更新
                this.dataset.favorited = (!isFavorited).toString();
                const starSpan = this.querySelector('.star');
                const textSpan = this.querySelector('.text');

                if (starSpan) {
                    starSpan.textContent = isFavorited ? '☆' : '★';
                }
                if (textSpan) {
                    textSpan.textContent = isFavorited
                        ? 'お気に入り登録する'
                        : 'お気に入り登録済み';
                }

                if (!isFavorited) {
                    this.classList.add('favorited');
                } else {
                    this.classList.remove('favorited');
                }
            } catch (error) {
                console.error('お気に入り状態の変更エラー:', error);
                alert('エラーが発生しました。もう一度お試しください。');
            }
        });
    });
});
